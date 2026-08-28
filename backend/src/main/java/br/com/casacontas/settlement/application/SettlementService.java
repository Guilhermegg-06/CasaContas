package br.com.casacontas.settlement.application;

import br.com.casacontas.expense.application.ExpenseRepository;
import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.ExpenseShare;
import br.com.casacontas.household.application.HouseholdAccessService;
import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.settlement.domain.IdempotencyRecord;
import br.com.casacontas.settlement.domain.Settlement;
import br.com.casacontas.settlement.domain.SettlementType;
import br.com.casacontas.shared.application.AuditPort;
import br.com.casacontas.shared.application.BusinessException;
import br.com.casacontas.shared.domain.AuditEvent;
import br.com.casacontas.shared.infrastructure.security.OpaqueTokenSupport;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

  private final SettlementRepository settlements;
  private final IdempotencyRepository idempotency;
  private final ExpenseRepository expenses;
  private final HouseholdRepository households;
  private final HouseholdAccessService access;
  private final AuditPort audit;
  private final OpaqueTokenSupport hashes;
  private final Clock clock;

  public SettlementService(
      SettlementRepository settlements,
      IdempotencyRepository idempotency,
      ExpenseRepository expenses,
      HouseholdRepository households,
      HouseholdAccessService access,
      AuditPort audit,
      OpaqueTokenSupport hashes,
      Clock clock) {
    this.settlements = settlements;
    this.idempotency = idempotency;
    this.expenses = expenses;
    this.households = households;
    this.access = access;
    this.audit = audit;
    this.hashes = hashes;
    this.clock = clock;
  }

  @Transactional
  public Settlement registerPrimaryPayment(
      UUID userId, UUID householdId, UUID expenseId, UUID payerMemberId, String idempotencyKey) {
    requireKey(idempotencyKey);
    HouseholdMember actor = access.requireActiveMember(householdId, userId);
    HouseholdMember payer = requireMember(householdId, payerMemberId);
    if (!actor.role().canManageMembers() && !actor.id().equals(payer.id())) {
      throw BusinessException.forbidden("Um morador só pode registrar o próprio pagamento");
    }
    String operation = "PRIMARY_PAYMENT";
    String payloadHash = hashes.hash(expenseId + "|" + payerMemberId);
    Settlement repeated = repeated(householdId, userId, operation, idempotencyKey, payloadHash);
    if (repeated != null) {
      return repeated;
    }
    Expense expense = requireExpenseForUpdate(householdId, expenseId);
    Instant now = clock.instant();
    Expense paid = expense.registerPrimaryPayment(payerMemberId, now);
    expenses.save(paid);
    UUID payerShareId =
        paid.shares().stream()
            .filter(share -> share.memberId().equals(payerMemberId))
            .map(ExpenseShare::id)
            .findFirst()
            .orElse(null);
    Settlement settlement =
        settlements.save(
            new Settlement(
                UUID.randomUUID(),
                expenseId,
                payerShareId,
                householdId,
                userId,
                payerMemberId,
                null,
                expense.total(),
                SettlementType.PRIMARY_PAYMENT,
                null,
                now));
    recordIdempotency(householdId, userId, operation, idempotencyKey, payloadHash, settlement, now);
    appendAudit(userId, expense, "PRIMARY_PAYMENT_RECORDED", settlement.id(), now);
    return settlement;
  }

  @Transactional
  public Settlement settleShare(
      UUID userId, UUID householdId, UUID expenseId, UUID shareId, String idempotencyKey) {
    requireKey(idempotencyKey);
    HouseholdMember actor = access.requireActiveMember(householdId, userId);
    String operation = "SETTLE_SHARE";
    String payloadHash = hashes.hash(expenseId + "|" + shareId);
    Settlement repeated = repeated(householdId, userId, operation, idempotencyKey, payloadHash);
    if (repeated != null) {
      return repeated;
    }
    Expense expense = requireExpenseForUpdate(householdId, expenseId);
    ExpenseShare share =
        expense.shares().stream()
            .filter(candidate -> candidate.id().equals(shareId))
            .findFirst()
            .orElseThrow(BusinessException::notFound);
    if (!actor.role().canManageMembers() && !actor.id().equals(share.memberId())) {
      throw BusinessException.forbidden("Um morador só pode confirmar a própria parte");
    }
    Instant now = clock.instant();
    Expense settledExpense = expense.settleShare(shareId, now);
    expenses.save(settledExpense);
    UUID recipient = expense.paidByMemberId();
    SettlementType type =
        recipient == null ? SettlementType.SHARE_PAYMENT : SettlementType.REIMBURSEMENT;
    Settlement settlement =
        settlements.save(
            new Settlement(
                UUID.randomUUID(),
                expenseId,
                shareId,
                householdId,
                userId,
                share.memberId(),
                recipient,
                share.amount(),
                type,
                null,
                now));
    recordIdempotency(householdId, userId, operation, idempotencyKey, payloadHash, settlement, now);
    appendAudit(userId, expense, "SHARE_SETTLED", settlement.id(), now);
    return settlement;
  }

  @Transactional(readOnly = true)
  public java.util.List<Settlement> history(UUID userId, UUID householdId, UUID expenseId) {
    access.requireActiveMember(householdId, userId);
    Expense expense =
        expenses
            .findById(expenseId)
            .filter(candidate -> candidate.householdId().equals(householdId))
            .orElseThrow(BusinessException::notFound);
    return settlements.findByExpense(expense.id());
  }

  private Expense requireExpenseForUpdate(UUID householdId, UUID expenseId) {
    return expenses
        .findByIdForUpdate(expenseId)
        .filter(expense -> expense.householdId().equals(householdId))
        .orElseThrow(BusinessException::notFound);
  }

  private HouseholdMember requireMember(UUID householdId, UUID memberId) {
    return households
        .findMemberById(memberId)
        .filter(member -> member.householdId().equals(householdId))
        .filter(HouseholdMember::active)
        .orElseThrow(BusinessException::notFound);
  }

  private Settlement repeated(
      UUID householdId, UUID userId, String operation, String key, String payloadHash) {
    return idempotency
        .find(householdId, userId, operation, key)
        .map(
            record -> {
              if (!record.payloadHash().equals(payloadHash)) {
                throw BusinessException.conflict(
                    "IDEMPOTENCY_KEY_REUSED",
                    "A chave de idempotência já foi usada com outros dados");
              }
              return settlements
                  .findById(record.settlementId())
                  .orElseThrow(BusinessException::notFound);
            })
        .orElse(null);
  }

  private void recordIdempotency(
      UUID householdId,
      UUID userId,
      String operation,
      String key,
      String payloadHash,
      Settlement settlement,
      Instant now) {
    idempotency.save(
        new IdempotencyRecord(
            UUID.randomUUID(),
            householdId,
            userId,
            operation,
            key,
            payloadHash,
            settlement.id(),
            now));
  }

  private void appendAudit(
      UUID userId, Expense expense, String type, UUID settlementId, Instant now) {
    audit.append(
        new AuditEvent(
            UUID.randomUUID(),
            expense.householdId(),
            expense.id(),
            userId,
            type,
            "{\"settlementId\":\"" + settlementId + "\"}",
            now));
  }

  private void requireKey(String key) {
    if (key == null || key.isBlank() || key.length() > 100) {
      throw BusinessException.unprocessable(
          "IDEMPOTENCY_KEY_REQUIRED", "Informe uma Idempotency-Key válida");
    }
  }
}
