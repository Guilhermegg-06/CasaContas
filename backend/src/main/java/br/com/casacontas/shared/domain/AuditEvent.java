package br.com.casacontas.shared.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
    UUID id,
    UUID householdId,
    UUID expenseId,
    UUID actorUserId,
    String eventType,
    String details,
    Instant occurredAt) {}
