package br.com.casacontas.expense.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseShareJpaRepository extends JpaRepository<ExpenseShareEntity, UUID> {

  List<ExpenseShareEntity> findByExpenseIdAndActiveTrueOrderById(UUID expenseId);
}
