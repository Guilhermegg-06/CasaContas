package br.com.casacontas.expense.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpenseTest {

  private static final Instant CREATED = Instant.parse("2026-08-01T12:00:00Z");
  private final UUID payer = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private final ExpenseShare payerShare = share(payer, "33.34");
  private final ExpenseShare secondShare =
      share(UUID.fromString("10000000-0000-0000-0000-000000000002"), "33.33");
  private final ExpenseShare thirdShare =
      share(UUID.fromString("10000000-0000-0000-0000-000000000003"), "33.33");

  @Test
  void primaryPayerCoversOwnShareAndOtherResidentsStillOweTheirParts() {
    Expense paid = expense().registerPrimaryPayment(payer, CREATED.plusSeconds(60));

    assertThat(paid.paidByMemberId()).isEqualTo(payer);
    assertThat(paid.shares())
        .extracting(ExpenseShare::status)
        .containsExactly(ShareStatus.COVERED, ShareStatus.PENDING, ShareStatus.PENDING);
    assertThat(paid.status()).isEqualTo(ExpenseStatus.PENDING);
  }

  @Test
  void expenseSettlesOnlyAfterEveryRequiredShareIsSettled() {
    Expense paid = expense().registerPrimaryPayment(payer, CREATED.plusSeconds(60));
    Expense partiallySettled = paid.settleShare(secondShare.id(), CREATED.plusSeconds(120));
    Expense settled = partiallySettled.settleShare(thirdShare.id(), CREATED.plusSeconds(180));

    assertThat(partiallySettled.status()).isEqualTo(ExpenseStatus.PENDING);
    assertThat(settled.status()).isEqualTo(ExpenseStatus.SETTLED);
  }

  @Test
  void rejectsDuplicateFinancialConfirmationAndCalculatesOverdueWithoutChangingState() {
    Expense paid = expense().registerPrimaryPayment(payer, CREATED.plusSeconds(60));

    assertThatThrownBy(() -> paid.registerPrimaryPayment(payer, CREATED.plusSeconds(120)))
        .isInstanceOf(FinancialRuleException.class);
    assertThat(paid.displayStatus(LocalDate.of(2026, 9, 1))).isEqualTo("OVERDUE");
    assertThat(paid.status()).isEqualTo(ExpenseStatus.PENDING);
  }

  @Test
  void cancellationIsLogicalAndKeepsShares() {
    Expense cancelled = expense().cancel(CREATED.plusSeconds(60));

    assertThat(cancelled.status()).isEqualTo(ExpenseStatus.CANCELLED);
    assertThat(cancelled.cancelledAt()).isNotNull();
    assertThat(cancelled.shares()).containsExactly(payerShare, secondShare, thirdShare);
  }

  private Expense expense() {
    return new Expense(
        UUID.fromString("20000000-0000-0000-0000-000000000001"),
        UUID.fromString("30000000-0000-0000-0000-000000000001"),
        payer,
        null,
        "Energia",
        new BigDecimal("100.00"),
        "Moradia",
        LocalDate.of(2026, 8, 10),
        null,
        SplitType.EQUAL,
        "BRL",
        ExpenseStatus.PENDING,
        CREATED,
        CREATED,
        null,
        0,
        List.of(payerShare, secondShare, thirdShare));
  }

  private ExpenseShare share(UUID memberId, String amount) {
    return new ExpenseShare(
        UUID.randomUUID(),
        UUID.fromString("20000000-0000-0000-0000-000000000001"),
        memberId,
        new BigDecimal(amount),
        ShareStatus.PENDING,
        null,
        0);
  }
}
