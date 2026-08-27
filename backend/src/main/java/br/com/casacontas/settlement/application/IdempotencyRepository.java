package br.com.casacontas.settlement.application;

import br.com.casacontas.settlement.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

  Optional<IdempotencyRecord> find(
      UUID householdId, UUID actorUserId, String operation, String key);

  IdempotencyRecord save(IdempotencyRecord record);
}
