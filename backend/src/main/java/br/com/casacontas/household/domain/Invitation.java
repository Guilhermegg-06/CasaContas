package br.com.casacontas.household.domain;

import java.time.Instant;
import java.util.UUID;

public record Invitation(
    UUID id,
    UUID householdId,
    UUID invitedBy,
    MemberRole role,
    String tokenHash,
    Instant expiresAt,
    Instant createdAt,
    Instant usedAt,
    UUID usedBy) {

  public boolean canBeUsedAt(Instant instant) {
    return usedAt == null && expiresAt.isAfter(instant);
  }

  public Invitation use(UUID userId, Instant instant) {
    return new Invitation(
        id, householdId, invitedBy, role, tokenHash, expiresAt, createdAt, instant, userId);
  }
}
