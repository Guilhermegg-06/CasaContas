package br.com.casacontas.expense.application;

import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository {

  Expense save(Expense expense);

  Optional<Expense> findById(UUID expenseId);

  Optional<Expense> findByIdForUpdate(UUID expenseId);

  PageResult<Expense> findAll(ExpenseFilter filter);

  boolean hasFinancialMovement(UUID expenseId);
}
