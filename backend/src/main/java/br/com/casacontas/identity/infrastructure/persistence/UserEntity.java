package br.com.casacontas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

  @Id UUID id;

  @Column(nullable = false, length = 100)
  String name;

  @Column(nullable = false, length = 254, unique = true)
  String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  String passwordHash;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  protected UserEntity() {}
}
