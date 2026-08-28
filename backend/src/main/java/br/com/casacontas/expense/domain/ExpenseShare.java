package br.com.casacontas.expense.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseShare(
    UUID id,
    UUID expenseId,
    UUID memberId,
    BigDecimal amount,
    ShareStatus status,
    Instant settledAt,
    long version) {

  public ExpenseShare cover(Instant instant) {
    if (status != ShareStatus.PENDING) {
      throw new FinancialRuleException("A parte já está liquidada");
    }
    return new ExpenseShare(id, expenseId, memberId, amount, ShareStatus.COVERED, instant, version);
  }

  public ExpenseShare settle(Instant instant) {
    if (status != ShareStatus.PENDING) {
      throw new FinancialRuleException("A parte já está liquidada");
    }
    return new ExpenseShare(id, expenseId, memberId, amount, ShareStatus.SETTLED, instant, version);
  }
}
