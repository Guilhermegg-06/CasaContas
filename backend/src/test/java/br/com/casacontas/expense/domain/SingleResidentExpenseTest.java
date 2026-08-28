package br.com.casacontas.expense.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SingleResidentExpenseTest {

  @Test
  void settlesWhenTheOnlyResidentRegistersThePrimaryPayment() {
    Instant createdAt = Instant.parse("2026-08-01T12:00:00Z");
    UUID payerId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID expenseId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    ExpenseShare onlyShare =
        new ExpenseShare(
            UUID.fromString("40000000-0000-0000-0000-000000000001"),
            expenseId,
            payerId,
            new BigDecimal("99.90"),
            ShareStatus.PENDING,
            null,
            0);
    Expense expense =
        new Expense(
            expenseId,
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            payerId,
            null,
            "Internet",
            new BigDecimal("99.90"),
            "Moradia",
            LocalDate.of(2026, 8, 10),
            null,
            SplitType.EQUAL,
            "BRL",
            ExpenseStatus.PENDING,
            createdAt,
            createdAt,
            null,
            0,
            List.of(onlyShare));

    Expense paid = expense.registerPrimaryPayment(payerId, createdAt.plusSeconds(60));

    assertThat(paid.status()).isEqualTo(ExpenseStatus.SETTLED);
    assertThat(paid.shares()).extracting(ExpenseShare::status).containsExactly(ShareStatus.COVERED);
  }
}
