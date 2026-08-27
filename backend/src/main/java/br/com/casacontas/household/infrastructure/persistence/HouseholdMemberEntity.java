package br.com.casacontas.household.infrastructure.persistence;

import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.household.domain.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household_members")
class HouseholdMemberEntity {

  @Id UUID id;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "user_id", nullable = false)
  UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  MemberRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  MemberStatus status;

  @Column(name = "joined_at", nullable = false)
  Instant joinedAt;

  @Column(name = "removed_at")
  Instant removedAt;

  protected HouseholdMemberEntity() {}
}
