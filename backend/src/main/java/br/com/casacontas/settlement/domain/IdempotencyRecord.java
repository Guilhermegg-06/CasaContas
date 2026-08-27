package br.com.casacontas.settlement.domain;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
    UUID id,
    UUID householdId,
    UUID actorUserId,
    String operation,
    String key,
    String payloadHash,
    UUID settlementId,
    Instant createdAt) {}
