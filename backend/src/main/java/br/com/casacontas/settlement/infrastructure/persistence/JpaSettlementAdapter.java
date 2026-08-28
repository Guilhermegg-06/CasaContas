package br.com.casacontas.settlement.infrastructure.persistence;

import br.com.casacontas.settlement.application.IdempotencyRepository;
import br.com.casacontas.settlement.application.SettlementRepository;
import br.com.casacontas.settlement.domain.IdempotencyRecord;
import br.com.casacontas.settlement.domain.Settlement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSettlementAdapter implements SettlementRepository, IdempotencyRepository {

  private final SettlementJpaRepository settlements;
  private final IdempotencyJpaRepository idempotency;

  public JpaSettlementAdapter(
      SettlementJpaRepository settlements, IdempotencyJpaRepository idempotency) {
    this.settlements = settlements;
    this.idempotency = idempotency;
  }

  @Override
  public Settlement save(Settlement settlement) {
    SettlementEntity entity = new SettlementEntity();
    entity.id = settlement.id();
    entity.expenseId = settlement.expenseId();
    entity.shareId = settlement.shareId();
    entity.householdId = settlement.householdId();
    entity.actorUserId = settlement.actorUserId();
    entity.payerMemberId = settlement.payerMemberId();
    entity.recipientMemberId = settlement.recipientMemberId();
    entity.amount = settlement.amount();
    entity.type = settlement.type();
    entity.reversesSettlementId = settlement.reversesSettlementId();
    entity.occurredAt = settlement.occurredAt();
    return toDomain(settlements.save(entity));
  }

  @Override
  public Optional<Settlement> findById(UUID id) {
    return settlements.findById(id).map(this::toDomain);
  }

  @Override
  public List<Settlement> findByExpense(UUID expenseId) {
    return settlements.findByExpenseIdOrderByOccurredAtAsc(expenseId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<IdempotencyRecord> find(
      UUID householdId, UUID actorUserId, String operation, String key) {
    return idempotency
        .findByHouseholdIdAndActorUserIdAndOperationAndKey(householdId, actorUserId, operation, key)
        .map(this::toDomain);
  }

  @Override
  public IdempotencyRecord save(IdempotencyRecord record) {
    IdempotencyEntity entity = new IdempotencyEntity();
    entity.id = record.id();
    entity.householdId = record.householdId();
    entity.actorUserId = record.actorUserId();
    entity.operation = record.operation();
    entity.key = record.key();
    entity.payloadHash = record.payloadHash();
    entity.settlementId = record.settlementId();
    entity.createdAt = record.createdAt();
    return toDomain(idempotency.save(entity));
  }

  private Settlement toDomain(SettlementEntity entity) {
    return new Settlement(
        entity.id,
        entity.expenseId,
        entity.shareId,
        entity.householdId,
        entity.actorUserId,
        entity.payerMemberId,
        entity.recipientMemberId,
        entity.amount,
        entity.type,
        entity.reversesSettlementId,
        entity.occurredAt);
  }

  private IdempotencyRecord toDomain(IdempotencyEntity entity) {
    return new IdempotencyRecord(
        entity.id,
        entity.householdId,
        entity.actorUserId,
        entity.operation,
        entity.key,
        entity.payloadHash,
        entity.settlementId,
        entity.createdAt);
  }
}
