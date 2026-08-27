package br.com.casacontas.expense.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SplitCalculatorTest {

  private final SplitCalculator calculator = new SplitCalculator();
  private final UUID ana = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final UUID bia = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private final UUID caio = UUID.fromString("00000000-0000-0000-0000-000000000003");

  @Test
  void distributesRemainingCentsDeterministicallyWithoutChangingTheTotal() {
    Map<UUID, BigDecimal> shares =
        calculator.equal(new BigDecimal("100.00"), List.of(ana, bia, caio));

    assertThat(shares)
        .containsExactly(
            Map.entry(ana, new BigDecimal("33.34")),
            Map.entry(bia, new BigDecimal("33.33")),
            Map.entry(caio, new BigDecimal("33.33")));
    assertThat(shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("100.00");
  }

  @Test
  void rejectsCustomSharesWhoseSumDiffersFromTheExpenseTotal() {
    Map<UUID, BigDecimal> shares = new LinkedHashMap<>();
    shares.put(ana, new BigDecimal("50.00"));
    shares.put(bia, new BigDecimal("40.00"));
    shares.put(caio, new BigDecimal("20.00"));

    assertThatThrownBy(() -> calculator.custom(new BigDecimal("120.00"), shares))
        .isInstanceOf(FinancialRuleException.class)
        .hasMessageContaining("10,00");
  }

  @Test
  void rejectsDuplicateMembersAndValuesWithMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> calculator.equal(new BigDecimal("10.00"), List.of(ana, ana)))
        .isInstanceOf(FinancialRuleException.class);

    assertThatThrownBy(() -> calculator.equal(new BigDecimal("10.001"), List.of(ana)))
        .isInstanceOf(FinancialRuleException.class);
  }
}
