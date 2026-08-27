package br.com.casacontas.settlement.infrastructure.persistence;

import br.com.casacontas.settlement.domain.SettlementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlements")
class SettlementEntity {

  @Id UUID id;

  @Column(name = "expense_id", nullable = false)
  UUID expenseId;

  @Column(name = "share_id")
  UUID shareId;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "actor_user_id", nullable = false)
  UUID actorUserId;

  @Column(name = "payer_member_id", nullable = false)
  UUID payerMemberId;

  @Column(name = "recipient_member_id")
  UUID recipientMemberId;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  SettlementType type;

  @Column(name = "reverses_settlement_id")
  UUID reversesSettlementId;

  @Column(name = "occurred_at", nullable = false)
  Instant occurredAt;

  protected SettlementEntity() {}
}
