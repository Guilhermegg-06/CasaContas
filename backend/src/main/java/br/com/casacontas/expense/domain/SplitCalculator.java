package br.com.casacontas.expense.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SplitCalculator {

  private static final int MONEY_SCALE = 2;

  public Map<UUID, BigDecimal> equal(BigDecimal total, List<UUID> memberIds) {
    BigDecimal normalizedTotal = requirePositiveMoney(total, "valor total");
    requireUniqueParticipants(memberIds);

    long totalCents = normalizedTotal.movePointRight(MONEY_SCALE).longValueExact();
    long baseCents = totalCents / memberIds.size();
    long remainder = totalCents % memberIds.size();
    Map<UUID, BigDecimal> result = new LinkedHashMap<>();

    for (int index = 0; index < memberIds.size(); index++) {
      long cents = baseCents + (index < remainder ? 1 : 0);
      result.put(memberIds.get(index), BigDecimal.valueOf(cents, MONEY_SCALE));
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(result));
  }

  public Map<UUID, BigDecimal> custom(BigDecimal total, Map<UUID, BigDecimal> requestedShares) {
    BigDecimal normalizedTotal = requirePositiveMoney(total, "valor total");
    if (requestedShares == null || requestedShares.isEmpty()) {
      throw new FinancialRuleException("A despesa deve possuir pelo menos um participante");
    }

    Map<UUID, BigDecimal> normalized = new LinkedHashMap<>();
    requestedShares.forEach(
        (memberId, amount) -> {
          if (memberId == null) {
            throw new FinancialRuleException("O participante é obrigatório");
          }
          normalized.put(memberId, requirePositiveMoney(amount, "parte"));
        });

    BigDecimal sum = normalized.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(normalizedTotal) != 0) {
      BigDecimal difference = normalizedTotal.subtract(sum).abs();
      String formatted =
          NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(difference);
      throw new FinancialRuleException(
          "A soma das partes deve ser igual ao total; diferença de " + formatted);
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
  }

  private static BigDecimal requirePositiveMoney(BigDecimal value, String field) {
    if (value == null || value.signum() <= 0) {
      throw new FinancialRuleException("O " + field + " deve ser positivo");
    }
    try {
      return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException exception) {
      throw new FinancialRuleException(
          "O " + field + " deve possuir no máximo duas casas decimais");
    }
  }

  private static void requireUniqueParticipants(List<UUID> memberIds) {
    if (memberIds == null || memberIds.isEmpty()) {
      throw new FinancialRuleException("A despesa deve possuir pelo menos um participante");
    }
    if (memberIds.stream().anyMatch(java.util.Objects::isNull)) {
      throw new FinancialRuleException("O participante é obrigatório");
    }
    if (new HashSet<>(memberIds).size() != memberIds.size()) {
      throw new FinancialRuleException("Um morador não pode aparecer duas vezes na divisão");
    }
  }
}
