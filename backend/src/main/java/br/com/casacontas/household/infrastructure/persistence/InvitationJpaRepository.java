package br.com.casacontas.household.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvitationJpaRepository extends JpaRepository<InvitationEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select invitation from InvitationEntity invitation where invitation.tokenHash = :tokenHash")
  Optional<InvitationEntity> findForUpdate(@Param("tokenHash") String tokenHash);
}
