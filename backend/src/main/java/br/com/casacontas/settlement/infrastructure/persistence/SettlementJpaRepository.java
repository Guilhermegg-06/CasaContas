package br.com.casacontas.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementJpaRepository extends JpaRepository<SettlementEntity, UUID> {

  List<SettlementEntity> findByExpenseIdOrderByOccurredAtAsc(UUID expenseId);
}
