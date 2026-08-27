package br.com.casacontas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
class RefreshSessionEntity {

  @Id UUID id;

  @Column(name = "user_id", nullable = false)
  UUID userId;

  @Column(name = "token_hash", nullable = false, length = 64, unique = true)
  String tokenHash;

  @Column(name = "expires_at", nullable = false)
  Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "revoked_at")
  Instant revokedAt;

  @Column(name = "rotated_to")
  UUID rotatedTo;

  protected RefreshSessionEntity() {}
}
