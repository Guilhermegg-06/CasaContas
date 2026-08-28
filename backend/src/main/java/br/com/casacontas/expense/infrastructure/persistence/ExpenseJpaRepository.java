package br.com.casacontas.expense.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExpenseJpaRepository
    extends JpaRepository<ExpenseEntity, UUID>, JpaSpecificationExecutor<ExpenseEntity> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select expense from ExpenseEntity expense where expense.id = :id")
  Optional<ExpenseEntity> findForUpdate(@Param("id") UUID id);

  @Query(
      value = "select count(*) > 0 from settlements where expense_id = :expenseId",
      nativeQuery = true)
  boolean hasFinancialMovement(@Param("expenseId") UUID expenseId);
}
