package br.com.casacontas.settlement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
class IdempotencyEntity {

  @Id UUID id;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "actor_user_id", nullable = false)
  UUID actorUserId;

  @Column(nullable = false, length = 40)
  String operation;

  @Column(name = "idempotency_key", nullable = false, length = 100)
  String key;

  @Column(name = "payload_hash", nullable = false, length = 64)
  String payloadHash;

  @Column(name = "settlement_id", nullable = false)
  UUID settlementId;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  protected IdempotencyEntity() {}
}
