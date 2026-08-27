package br.com.casacontas.household.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "households")
class HouseholdEntity {

  @Id UUID id;

  @Column(nullable = false, length = 100)
  String name;

  @Column(nullable = false, length = 64)
  String timezone;

  @Column(nullable = false, length = 3)
  String currency;

  @Column(name = "created_by", nullable = false)
  UUID createdBy;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  protected HouseholdEntity() {}
}
