package br.com.casacontas.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    Instant createdAt,
    Instant revokedAt,
    UUID rotatedTo) {

  public boolean isActiveAt(Instant instant) {
    return revokedAt == null && expiresAt.isAfter(instant);
  }

  public RefreshSession rotateTo(UUID nextSessionId, Instant instant) {
    return new RefreshSession(id, userId, tokenHash, expiresAt, createdAt, instant, nextSessionId);
  }

  public RefreshSession revokeAt(Instant instant) {
    return new RefreshSession(id, userId, tokenHash, expiresAt, createdAt, instant, rotatedTo);
  }
}
