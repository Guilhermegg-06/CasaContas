package br.com.casacontas.expense.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialIntegrityTest {
  private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
  private static final UUID EXPENSE = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID ALICE = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID BRUNO = UUID.fromString("10000000-0000-0000-0000-000000000002");

  @Test
  void cancelledExpenseCannotBeRevivedBySettlingAShare() {
    Expense cancelled = expense().cancel(NOW);
    assertThatThrownBy(() -> cancelled.settleShare(BRUNO, NOW))
        .isInstanceOf(FinancialRuleException.class);
  }

  @Test
  void primaryPaymentCannotPayTheSupplierAgainAfterAnIndividualPayment() {
    Expense individuallyPaid = expense().settleShare(BRUNO, NOW);
    assertThatThrownBy(() -> individuallyPaid.registerPrimaryPayment(ALICE, NOW))
        .isInstanceOf(FinancialRuleException.class);
  }

  @Test
  void equalSplitSupportsTheDatabaseMoneyRangeWithoutOverflow() {
    var total = new BigDecimal("99999999999999999.99");
    var result = new SplitCalculator().equal(total, List.of(ALICE, BRUNO));
    assertThat(result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualTo(total);
    assertThat(result.get(ALICE)).isEqualTo(new BigDecimal("50000000000000000.00"));
  }

  private Expense expense() {
    return new Expense(
        EXPENSE,
        EXPENSE,
        ALICE,
        null,
        "Energia",
        new BigDecimal("100.00"),
        "Energia",
        LocalDate.of(2026, 9, 30),
        null,
        SplitType.EQUAL,
        "BRL",
        ExpenseStatus.PENDING,
        NOW,
        NOW,
        null,
        0,
        List.of(
            new ExpenseShare(
                ALICE, EXPENSE, ALICE, new BigDecimal("50.00"), ShareStatus.PENDING, null, 0),
            new ExpenseShare(
                BRUNO, EXPENSE, BRUNO, new BigDecimal("50.00"), ShareStatus.PENDING, null, 0)));
  }
}
