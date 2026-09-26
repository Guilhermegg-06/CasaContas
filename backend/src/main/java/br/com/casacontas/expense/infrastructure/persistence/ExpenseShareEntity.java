package br.com.casacontas.expense.infrastructure.persistence;

import br.com.casacontas.expense.domain.ShareStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense_shares")
class ExpenseShareEntity {

  @Id UUID id;

  @Column(name = "expense_id", nullable = false)
  UUID expenseId;

  @Column(name = "member_id", nullable = false)
  UUID memberId;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal amount;

  @Column(nullable = false)
  boolean active = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  ShareStatus status;

  @Column(name = "settled_at")
  Instant settledAt;

  @Version
  @Column(nullable = false)
  long version;

  protected ExpenseShareEntity() {}
}
