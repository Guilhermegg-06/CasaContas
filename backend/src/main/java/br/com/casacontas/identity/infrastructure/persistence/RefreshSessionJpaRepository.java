package br.com.casacontas.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select session from RefreshSessionEntity session where session.tokenHash = :tokenHash")
  Optional<RefreshSessionEntity> findForUpdate(@Param("tokenHash") String tokenHash);
}
