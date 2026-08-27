package br.com.casacontas.expense.api;

import br.com.casacontas.expense.application.ExpenseFilter;
import br.com.casacontas.expense.application.ExpenseService;
import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.ExpenseShare;
import br.com.casacontas.expense.domain.FinancialRuleException;
import br.com.casacontas.expense.domain.ShareStatus;
import br.com.casacontas.expense.domain.SplitType;
import br.com.casacontas.settlement.application.SettlementService;
import br.com.casacontas.settlement.domain.Settlement;
import br.com.casacontas.settlement.domain.SettlementType;
import br.com.casacontas.shared.application.AuditPort;
import br.com.casacontas.shared.application.CurrentUser;
import br.com.casacontas.shared.application.PageResult;
import br.com.casacontas.shared.domain.AuditEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/expenses")
public class ExpenseController {

  private final ExpenseService service;
  private final SettlementService settlementService;
  private final AuditPort audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  public ExpenseController(
      ExpenseService service,
      SettlementService settlementService,
      AuditPort audit,
      CurrentUser currentUser,
      Clock clock) {
    this.service = service;
    this.settlementService = settlementService;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @PostMapping
  ResponseEntity<ExpenseResponse> create(
      @PathVariable UUID householdId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody ExpenseRequest request) {
    UUID userId = currentUser.id();
    Expense expense = service.create(userId, request.toCommand(householdId));
    if (request.paidByMemberId() != null) {
      settlementService.registerPrimaryPayment(
          userId, householdId, expense.id(), request.paidByMemberId(), idempotencyKey);
      expense = service.detail(userId, householdId, expense.id());
    }
    ExpenseResponse response = toResponse(expense, true);
    return ResponseEntity.created(
            URI.create("/api/v1/households/" + householdId + "/expenses/" + expense.id()))
        .body(response);
  }

  @GetMapping
  PageResult<ExpenseResponse> list(
      @PathVariable UUID householdId,
      @RequestParam(required = false) YearMonth month,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID memberId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "dueDate") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    int safeSize = Math.max(1, Math.min(size, 100));
    ExpenseFilter filter =
        new ExpenseFilter(
            householdId,
            month,
            category,
            status,
            memberId,
            Math.max(0, page),
            safeSize,
            sort,
            "asc".equalsIgnoreCase(direction));
    PageResult<Expense> result = service.list(currentUser.id(), filter);
    return new PageResult<>(
        result.content().stream().map(expense -> toResponse(expense, false)).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }

  @GetMapping("/{expenseId}")
  ExpenseResponse detail(@PathVariable UUID householdId, @PathVariable UUID expenseId) {
    return toResponse(service.detail(currentUser.id(), householdId, expenseId), true);
  }

  @PutMapping("/{expenseId}")
  ExpenseResponse update(
      @PathVariable UUID householdId,
      @PathVariable UUID expenseId,
      @Valid @RequestBody ExpenseRequest request) {
    return toResponse(
        service.update(currentUser.id(), householdId, expenseId, request.toCommand(householdId)),
        true);
  }

  @DeleteMapping("/{expenseId}")
  ResponseEntity<Void> cancel(@PathVariable UUID householdId, @PathVariable UUID expenseId) {
    service.cancel(currentUser.id(), householdId, expenseId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{expenseId}/primary-payment")
  SettlementResponse registerPrimaryPayment(
      @PathVariable UUID householdId,
      @PathVariable UUID expenseId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody PrimaryPaymentRequest request) {
    return SettlementResponse.from(
        settlementService.registerPrimaryPayment(
            currentUser.id(), householdId, expenseId, request.payerMemberId(), idempotencyKey));
  }

  @PostMapping("/{expenseId}/shares/{shareId}/settlements")
  SettlementResponse settleShare(
      @PathVariable UUID householdId,
      @PathVariable UUID expenseId,
      @PathVariable UUID shareId,
      @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return SettlementResponse.from(
        settlementService.settleShare(
            currentUser.id(), householdId, expenseId, shareId, idempotencyKey));
  }

  private ExpenseResponse toResponse(Expense expense, boolean includeHistory) {
    List<AuditResponse> auditEvents =
        includeHistory
            ? audit.findByExpense(expense.id()).stream().map(AuditResponse::from).toList()
            : List.of();
    List<SettlementResponse> settlements =
        includeHistory
            ? settlementService
                .history(currentUser.id(), expense.householdId(), expense.id())
                .stream()
                .map(SettlementResponse::from)
                .toList()
            : List.of();
    LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    return new ExpenseResponse(
        expense.id(),
        expense.householdId(),
        expense.createdByMemberId(),
        expense.paidByMemberId(),
        expense.title(),
        expense.total(),
        expense.category(),
        expense.dueDate(),
        expense.notes(),
        expense.splitType(),
        expense.currency(),
        expense.displayStatus(today),
        expense.createdAt(),
        expense.updatedAt(),
        expense.shares().stream().map(ShareResponse::from).toList(),
        settlements,
        auditEvents);
  }

  record ExpenseRequest(
      @NotBlank @Size(max = 120) String title,
      @NotNull @DecimalMin(value = "0.01") BigDecimal total,
      @NotBlank @Size(max = 40) String category,
      @NotNull LocalDate dueDate,
      @Size(max = 500) String notes,
      @NotNull SplitType splitType,
      @NotEmpty @Size(max = 100) List<@Valid ParticipantRequest> participants,
      UUID paidByMemberId) {

    ExpenseService.CreateExpense toCommand(UUID householdId) {
      List<UUID> participantIds = participants.stream().map(ParticipantRequest::memberId).toList();
      Map<UUID, BigDecimal> custom = new LinkedHashMap<>();
      if (splitType == SplitType.CUSTOM) {
        for (ParticipantRequest participant : participants) {
          if (custom.put(participant.memberId(), participant.amount()) != null) {
            throw new FinancialRuleException("Um morador não pode aparecer duas vezes na divisão");
          }
        }
      }
      return new ExpenseService.CreateExpense(
          householdId, title, total, category, dueDate, notes, splitType, participantIds, custom);
    }
  }

  record ParticipantRequest(
      @NotNull UUID memberId, @DecimalMin(value = "0.01") BigDecimal amount) {}

  record PrimaryPaymentRequest(@NotNull UUID payerMemberId) {}

  record ExpenseResponse(
      UUID id,
      UUID householdId,
      UUID createdByMemberId,
      UUID paidByMemberId,
      String title,
      BigDecimal total,
      String category,
      LocalDate dueDate,
      String notes,
      SplitType splitType,
      String currency,
      String status,
      Instant createdAt,
      Instant updatedAt,
      List<ShareResponse> shares,
      List<SettlementResponse> settlements,
      List<AuditResponse> auditEvents) {}

  record ShareResponse(
      UUID id, UUID memberId, BigDecimal amount, ShareStatus status, Instant settledAt) {
    static ShareResponse from(ExpenseShare share) {
      return new ShareResponse(
          share.id(), share.memberId(), share.amount(), share.status(), share.settledAt());
    }
  }

  record SettlementResponse(
      UUID id,
      UUID shareId,
      UUID payerMemberId,
      UUID recipientMemberId,
      BigDecimal amount,
      SettlementType type,
      Instant occurredAt) {
    static SettlementResponse from(Settlement settlement) {
      return new SettlementResponse(
          settlement.id(),
          settlement.shareId(),
          settlement.payerMemberId(),
          settlement.recipientMemberId(),
          settlement.amount(),
          settlement.type(),
          settlement.occurredAt());
    }
  }

  record AuditResponse(UUID id, String eventType, String details, Instant occurredAt) {
    static AuditResponse from(AuditEvent event) {
      return new AuditResponse(event.id(), event.eventType(), event.details(), event.occurredAt());
    }
  }
}
