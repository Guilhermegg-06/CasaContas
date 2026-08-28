package br.com.casacontas.shared.infrastructure.persistence;

import br.com.casacontas.shared.application.AuditPort;
import br.com.casacontas.shared.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuditAdapter implements AuditPort {

  private final AuditEventJpaRepository repository;

  public JpaAuditAdapter(AuditEventJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void append(AuditEvent event) {
    AuditEventEntity entity = new AuditEventEntity();
    entity.id = event.id();
    entity.householdId = event.householdId();
    entity.expenseId = event.expenseId();
    entity.actorUserId = event.actorUserId();
    entity.eventType = event.eventType();
    entity.details = event.details();
    entity.occurredAt = event.occurredAt();
    repository.save(entity);
  }

  @Override
  public List<AuditEvent> findByExpense(UUID expenseId) {
    return repository.findByExpenseIdOrderByOccurredAtAsc(expenseId).stream()
        .map(
            entity ->
                new AuditEvent(
                    entity.id,
                    entity.householdId,
                    entity.expenseId,
                    entity.actorUserId,
                    entity.eventType,
                    entity.details,
                    entity.occurredAt))
        .toList();
  }
}
