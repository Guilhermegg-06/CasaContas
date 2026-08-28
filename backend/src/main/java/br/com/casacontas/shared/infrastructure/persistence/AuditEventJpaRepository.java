package br.com.casacontas.shared.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {

  List<AuditEventEntity> findByExpenseIdOrderByOccurredAtAsc(UUID expenseId);
}
