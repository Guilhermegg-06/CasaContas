package br.com.casacontas.settlement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, UUID> {

  Optional<IdempotencyEntity> findByHouseholdIdAndActorUserIdAndOperationAndKey(
      UUID householdId, UUID actorUserId, String operation, String key);
}
