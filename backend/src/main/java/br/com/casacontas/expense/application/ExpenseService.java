package br.com.casacontas.expense.application;

import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.ExpenseShare;
import br.com.casacontas.expense.domain.ExpenseStatus;
import br.com.casacontas.expense.domain.ShareStatus;
import br.com.casacontas.expense.domain.SplitCalculator;
import br.com.casacontas.expense.domain.SplitType;
import br.com.casacontas.household.application.HouseholdAccessService;
import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.settlement.application.SettlementService;
import br.com.casacontas.shared.application.AuditPort;
import br.com.casacontas.shared.application.BusinessException;
import br.com.casacontas.shared.application.PageResult;
import br.com.casacontas.shared.domain.AuditEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

  private final ExpenseRepository expenses;
  private final HouseholdRepository households;
  private final HouseholdAccessService access;
  private final AuditPort audit;
  private final Clock clock;
  private final SettlementService settlements;
  private final SplitCalculator calculator = new SplitCalculator();

  public ExpenseService(
      ExpenseRepository expenses,
      HouseholdRepository households,
      HouseholdAccessService access,
      AuditPort audit,
      Clock clock,
      SettlementService settlements) {
    this.expenses = expenses;
    this.households = households;
    this.access = access;
    this.audit = audit;
    this.clock = clock;
    this.settlements = settlements;
  }

  @Transactional
  public Expense create(
      UUID userId, CreateExpense command, UUID payerMemberId, String idempotencyKey) {
    Expense expense = create(userId, command);
    if (payerMemberId != null) {
      settlements.registerPrimaryPayment(
          userId, command.householdId(), expense.id(), payerMemberId, idempotencyKey);
      return detail(userId, command.householdId(), expense.id());
    }
    return expense;
  }

  @Transactional
  public Expense create(UUID userId, CreateExpense command) {
    HouseholdMember creator = access.requireActiveMember(command.householdId(), userId);
    List<HouseholdMember> participants =
        requireParticipants(command.householdId(), command.participantIds());
    Map<UUID, BigDecimal> amounts =
        command.splitType() == SplitType.EQUAL
            ? calculator.equal(command.total(), command.participantIds())
            : calculator.custom(command.total(), command.customShares());
    Instant now = clock.instant();
    UUID expenseId = UUID.randomUUID();
    List<ExpenseShare> shares =
        participants.stream()
            .map(
                member ->
                    new ExpenseShare(
                        UUID.randomUUID(),
                        expenseId,
                        member.id(),
                        amounts.get(member.id()),
                        ShareStatus.PENDING,
                        null,
                        0))
            .toList();
    Expense expense =
        expenses.save(
            new Expense(
                expenseId,
                command.householdId(),
                creator.id(),
                null,
                command.title().trim(),
                command.total(),
                command.category().trim(),
                command.dueDate(),
                normalizeNotes(command.notes()),
                command.splitType(),
                "BRL",
                ExpenseStatus.PENDING,
                now,
                now,
                null,
                0,
                shares));
    appendAudit(
        userId, expense, "EXPENSE_CREATED", "{\"splitType\":\"" + command.splitType() + "\"}");
    return expense;
  }

  @Transactional(readOnly = true)
  public Expense detail(UUID userId, UUID householdId, UUID expenseId) {
    access.requireActiveMember(householdId, userId);
    return expenses
        .findById(expenseId)
        .filter(expense -> expense.householdId().equals(householdId))
        .orElseThrow(BusinessException::notFound);
  }

  @Transactional(readOnly = true)
  public PageResult<Expense> list(UUID userId, ExpenseFilter filter) {
    access.requireActiveMember(filter.householdId(), userId);
    if (filter.status() != null
        && !filter.status().isBlank()
        && !java.util.Set.of("PENDING", "OVERDUE", "SETTLED", "CANCELLED")
            .contains(filter.status())) {
      throw new BusinessException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "INVALID_STATUS",
          "Informe um status de despesa válido");
    }
    return expenses.findAll(filter);
  }

  @Transactional
  public Expense update(UUID userId, UUID householdId, UUID expenseId, CreateExpense command) {
    HouseholdMember actor = access.requireActiveMember(householdId, userId);
    Expense current = requireForUpdate(householdId, expenseId);
    boolean manager = actor.role().canManageMembers();
    if (!manager && !current.createdByMemberId().equals(actor.id())) {
      throw BusinessException.forbidden("Somente quem criou a despesa ou um gestor pode editá-la");
    }
    if (current.status() != ExpenseStatus.PENDING || expenses.hasFinancialMovement(expenseId)) {
      throw BusinessException.conflict(
          "EXPENSE_HAS_FINANCIAL_MOVEMENT",
          "Despesas com movimentação financeira não podem ter valor ou participantes alterados");
    }
    List<HouseholdMember> participants = requireParticipants(householdId, command.participantIds());
    Map<UUID, BigDecimal> amounts =
        command.splitType() == SplitType.EQUAL
            ? calculator.equal(command.total(), command.participantIds())
            : calculator.custom(command.total(), command.customShares());
    Instant now = clock.instant();
    List<ExpenseShare> shares =
        participants.stream()
            .map(
                member ->
                    new ExpenseShare(
                        UUID.randomUUID(),
                        expenseId,
                        member.id(),
                        amounts.get(member.id()),
                        ShareStatus.PENDING,
                        null,
                        0))
            .toList();
    Expense updated =
        expenses.save(
            new Expense(
                current.id(),
                current.householdId(),
                current.createdByMemberId(),
                null,
                command.title().trim(),
                command.total(),
                command.category().trim(),
                command.dueDate(),
                normalizeNotes(command.notes()),
                command.splitType(),
                "BRL",
                ExpenseStatus.PENDING,
                current.createdAt(),
                now,
                null,
                current.version(),
                shares));
    appendAudit(userId, updated, "EXPENSE_UPDATED", "{}");
    return updated;
  }

  @Transactional
  public Expense cancel(UUID userId, UUID householdId, UUID expenseId) {
    access.requireManager(householdId, userId);
    Expense expense = requireForUpdate(householdId, expenseId).cancel(clock.instant());
    Expense saved = expenses.save(expense);
    appendAudit(userId, saved, "EXPENSE_CANCELLED", "{}");
    return saved;
  }

  private List<HouseholdMember> requireParticipants(UUID householdId, List<UUID> participantIds) {
    if (participantIds == null) {
      throw BusinessException.unprocessable(
          "PARTICIPANTS_REQUIRED", "Selecione ao menos um participante");
    }
    return participantIds.stream()
        .map(
            id ->
                households
                    .findMemberById(id)
                    .filter(member -> member.householdId().equals(householdId))
                    .filter(HouseholdMember::active)
                    .orElseThrow(BusinessException::notFound))
        .toList();
  }

  private Expense requireForUpdate(UUID householdId, UUID expenseId) {
    return expenses
        .findByIdForUpdate(expenseId)
        .filter(expense -> expense.householdId().equals(householdId))
        .orElseThrow(BusinessException::notFound);
  }

  private String normalizeNotes(String notes) {
    return notes == null || notes.isBlank() ? null : notes.trim();
  }

  private void appendAudit(UUID actorUserId, Expense expense, String type, String details) {
    audit.append(
        new AuditEvent(
            UUID.randomUUID(),
            expense.householdId(),
            expense.id(),
            actorUserId,
            type,
            details,
            clock.instant()));
  }

  public record CreateExpense(
      UUID householdId,
      String title,
      BigDecimal total,
      String category,
      java.time.LocalDate dueDate,
      String notes,
      SplitType splitType,
      List<UUID> participantIds,
      Map<UUID, BigDecimal> customShares) {

    public CreateExpense {
      participantIds = List.copyOf(participantIds);
      customShares =
          java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(customShares));
    }
  }
}
