package br.com.casacontas.household.domain;

import java.time.Instant;
import java.util.UUID;

public record HouseholdMember(
    UUID id,
    UUID householdId,
    UUID userId,
    String userName,
    String userEmail,
    MemberRole role,
    MemberStatus status,
    Instant joinedAt,
    Instant removedAt) {

  public boolean active() {
    return status == MemberStatus.ACTIVE;
  }

  public HouseholdMember remove(Instant instant) {
    return new HouseholdMember(
        id,
        householdId,
        userId,
        userName,
        userEmail,
        role,
        MemberStatus.REMOVED,
        joinedAt,
        instant);
  }
}
