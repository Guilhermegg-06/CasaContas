package br.com.casacontas.notification.application;

import br.com.casacontas.expense.application.ExpenseRepository;
import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.ExpenseShare;
import br.com.casacontas.household.application.HouseholdAccessService;
import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.shared.application.BusinessException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeMessageService {

  private final ExpenseRepository expenses;
  private final HouseholdRepository households;
  private final HouseholdAccessService access;

  public ChargeMessageService(
      ExpenseRepository expenses, HouseholdRepository households, HouseholdAccessService access) {
    this.expenses = expenses;
    this.households = households;
    this.access = access;
  }

  @Transactional(readOnly = true)
  public ChargeMessage generate(UUID userId, UUID householdId, UUID expenseId, UUID shareId) {
    access.requireActiveMember(householdId, userId);
    Expense expense =
        expenses
            .findById(expenseId)
            .filter(candidate -> candidate.householdId().equals(householdId))
            .orElseThrow(BusinessException::notFound);
    ExpenseShare share =
        expense.shares().stream()
            .filter(candidate -> candidate.id().equals(shareId))
            .findFirst()
            .orElseThrow(BusinessException::notFound);
    HouseholdMember member =
        households.findMemberById(share.memberId()).orElseThrow(BusinessException::notFound);
    String money =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(share.amount());
    String dueDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(expense.dueDate());
    String name = member.userName() == null ? "Olá" : "Olá, " + member.userName();
    String message =
        "%s! Sua parte da despesa \"%s\" é %s e vence em %s. Identificação: %s."
            .formatted(name, expense.title(), money, dueDate, expense.id());
    return new ChargeMessage(expense.id(), share.id(), message);
  }

  public record ChargeMessage(UUID expenseId, UUID shareId, String message) {}
}
