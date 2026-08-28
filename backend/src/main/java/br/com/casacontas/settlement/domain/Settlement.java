package br.com.casacontas.settlement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Settlement(
    UUID id,
    UUID expenseId,
    UUID shareId,
    UUID householdId,
    UUID actorUserId,
    UUID payerMemberId,
    UUID recipientMemberId,
    BigDecimal amount,
    SettlementType type,
    UUID reversesSettlementId,
    Instant occurredAt) {}
