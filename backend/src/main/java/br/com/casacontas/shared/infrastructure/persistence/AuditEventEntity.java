package br.com.casacontas.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
class AuditEventEntity {

  @Id UUID id;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "expense_id")
  UUID expenseId;

  @Column(name = "actor_user_id", nullable = false)
  UUID actorUserId;

  @Column(name = "event_type", nullable = false)
  String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  String details;

  @Column(name = "occurred_at", nullable = false)
  Instant occurredAt;

  protected AuditEventEntity() {}
}
