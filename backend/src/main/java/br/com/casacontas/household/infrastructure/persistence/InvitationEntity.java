package br.com.casacontas.household.infrastructure.persistence;

import br.com.casacontas.household.domain.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
class InvitationEntity {

  @Id UUID id;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "invited_by", nullable = false)
  UUID invitedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  MemberRole role;

  @Column(name = "token_hash", nullable = false, length = 64, unique = true)
  String tokenHash;

  @Column(name = "expires_at", nullable = false)
  Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "used_at")
  Instant usedAt;

  @Column(name = "used_by")
  UUID usedBy;

  protected InvitationEntity() {}
}
