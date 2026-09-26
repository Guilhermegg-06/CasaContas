package br.com.casacontas.expense.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Expense(
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
    ExpenseStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant cancelledAt,
    long version,
    List<ExpenseShare> shares) {

  public Expense {
    shares = List.copyOf(shares);
  }

  public Expense registerPrimaryPayment(UUID payerMemberId, Instant instant) {
    if (status != ExpenseStatus.PENDING
        || paidByMemberId != null
        || shares.stream().anyMatch(share -> share.status() != ShareStatus.PENDING)) {
      throw new FinancialRuleException(
          "O pagamento principal exige uma despesa pendente sem pagamentos individuais");
    }
    List<ExpenseShare> updatedShares = new ArrayList<>(shares.size());
    for (ExpenseShare share : shares) {
      updatedShares.add(share.memberId().equals(payerMemberId) ? share.cover(instant) : share);
    }
    boolean allSettled =
        updatedShares.stream().noneMatch(share -> share.status() == ShareStatus.PENDING);
    return copy(
        payerMemberId,
        allSettled ? ExpenseStatus.SETTLED : ExpenseStatus.PENDING,
        instant,
        cancelledAt,
        updatedShares);
  }

  public Expense settleShare(UUID shareId, Instant instant) {
    if (status != ExpenseStatus.PENDING) {
      throw new FinancialRuleException("Somente despesas pendentes podem receber pagamentos");
    }
    List<ExpenseShare> updatedShares =
        shares.stream()
            .map(share -> share.id().equals(shareId) ? share.settle(instant) : share)
            .toList();
    if (updatedShares.equals(shares)) {
      throw new FinancialRuleException("Parte da despesa não encontrada");
    }
    boolean allSettled =
        updatedShares.stream().noneMatch(share -> share.status() == ShareStatus.PENDING);
    return copy(
        paidByMemberId,
        allSettled ? ExpenseStatus.SETTLED : ExpenseStatus.PENDING,
        instant,
        cancelledAt,
        updatedShares);
  }

  public Expense cancel(Instant instant) {
    if (status == ExpenseStatus.CANCELLED) {
      return this;
    }
    return copy(paidByMemberId, ExpenseStatus.CANCELLED, instant, instant, shares);
  }

  public String displayStatus(LocalDate today) {
    if (status == ExpenseStatus.CANCELLED) {
      return "CANCELLED";
    }
    if (status == ExpenseStatus.SETTLED) {
      return "SETTLED";
    }
    return dueDate.isBefore(today) ? "OVERDUE" : "PENDING";
  }

  private Expense copy(
      UUID payer,
      ExpenseStatus nextStatus,
      Instant updated,
      Instant cancelled,
      List<ExpenseShare> nextShares) {
    return new Expense(
        id,
        householdId,
        createdByMemberId,
        payer,
        title,
        total,
        category,
        dueDate,
        notes,
        splitType,
        currency,
        nextStatus,
        createdAt,
        updated,
        cancelled,
        version,
        nextShares);
  }
}
