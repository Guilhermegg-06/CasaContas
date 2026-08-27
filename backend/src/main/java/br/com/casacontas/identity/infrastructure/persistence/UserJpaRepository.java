package br.com.casacontas.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);
}
