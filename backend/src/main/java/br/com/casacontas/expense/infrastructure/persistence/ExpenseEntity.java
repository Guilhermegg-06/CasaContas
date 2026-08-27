package br.com.casacontas.expense.infrastructure.persistence;

import br.com.casacontas.expense.domain.ExpenseStatus;
import br.com.casacontas.expense.domain.SplitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses")
class ExpenseEntity {

  @Id UUID id;

  @Column(name = "household_id", nullable = false)
  UUID householdId;

  @Column(name = "created_by_member_id", nullable = false)
  UUID createdByMemberId;

  @Column(name = "paid_by_member_id")
  UUID paidByMemberId;

  @Column(nullable = false, length = 120)
  String title;

  @Column(nullable = false, precision = 19, scale = 2)
  BigDecimal total;

  @Column(nullable = false, length = 40)
  String category;

  @Column(name = "due_date", nullable = false)
  LocalDate dueDate;

  @Column(length = 500)
  String notes;

  @Enumerated(EnumType.STRING)
  @Column(name = "split_type", nullable = false, length = 16)
  SplitType splitType;

  @Column(nullable = false, length = 3)
  String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  ExpenseStatus status;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Column(name = "cancelled_at")
  Instant cancelledAt;

  @Version
  @Column(nullable = false)
  long version;

  protected ExpenseEntity() {}
}
