package br.com.casacontas.expense.infrastructure.persistence;

import br.com.casacontas.expense.application.ExpenseFilter;
import br.com.casacontas.expense.application.ExpenseRepository;
import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.ExpenseShare;
import br.com.casacontas.expense.domain.ExpenseStatus;
import br.com.casacontas.shared.application.PageResult;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExpenseAdapter implements ExpenseRepository {

  private static final Set<String> ALLOWED_SORTS = Set.of("dueDate", "createdAt", "total", "title");

  private final ExpenseJpaRepository expenses;
  private final ExpenseShareJpaRepository shares;

  public JpaExpenseAdapter(ExpenseJpaRepository expenses, ExpenseShareJpaRepository shares) {
    this.expenses = expenses;
    this.shares = shares;
  }

  @Override
  public Expense save(Expense expense) {
    ExpenseEntity entity = expenses.findById(expense.id()).orElseGet(ExpenseEntity::new);
    entity.id = expense.id();
    entity.householdId = expense.householdId();
    entity.createdByMemberId = expense.createdByMemberId();
    entity.paidByMemberId = expense.paidByMemberId();
    entity.title = expense.title();
    entity.total = expense.total();
    entity.category = expense.category();
    entity.dueDate = expense.dueDate();
    entity.notes = expense.notes();
    entity.splitType = expense.splitType();
    entity.currency = expense.currency();
    entity.status = expense.status();
    entity.createdAt = expense.createdAt();
    entity.updatedAt = expense.updatedAt();
    entity.cancelledAt = expense.cancelledAt();
    ExpenseEntity savedExpense = expenses.save(entity);

    List<ExpenseShareEntity> existing = shares.findByExpenseIdOrderById(expense.id());
    Set<UUID> requestedIds =
        expense.shares().stream()
            .map(ExpenseShare::id)
            .collect(java.util.stream.Collectors.toSet());
    Set<UUID> existingIds =
        existing.stream().map(row -> row.id).collect(java.util.stream.Collectors.toSet());
    if (!existingIds.equals(requestedIds)) {
      shares.deleteByExpenseId(expense.id());
      shares.flush();
      existing = List.of();
    }
    java.util.Map<UUID, ExpenseShareEntity> byId =
        existing.stream().collect(java.util.stream.Collectors.toMap(row -> row.id, row -> row));
    List<ExpenseShareEntity> savedShares =
        expense.shares().stream()
            .map(share -> toEntity(share, byId.get(share.id())))
            .map(shares::save)
            .toList();
    return toDomain(savedExpense, savedShares);
  }

  @Override
  public Optional<Expense> findById(UUID expenseId) {
    return expenses.findById(expenseId).map(this::withShares);
  }

  @Override
  public Optional<Expense> findByIdForUpdate(UUID expenseId) {
    return expenses.findForUpdate(expenseId).map(this::withShares);
  }

  @Override
  public PageResult<Expense> findAll(ExpenseFilter filter) {
    String sortProperty = ALLOWED_SORTS.contains(filter.sort()) ? filter.sort() : "dueDate";
    Sort.Direction direction = filter.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
    PageRequest pageable =
        PageRequest.of(filter.page(), filter.size(), Sort.by(direction, sortProperty));
    Page<ExpenseEntity> page = expenses.findAll(specification(filter), pageable);
    return new PageResult<>(
        page.getContent().stream().map(this::withShares).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  @Override
  public boolean hasFinancialMovement(UUID expenseId) {
    return expenses.hasFinancialMovement(expenseId);
  }

  private Specification<ExpenseEntity> specification(ExpenseFilter filter) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("householdId"), filter.householdId()));
      if (filter.month() != null) {
        predicates.add(
            builder.between(
                root.get("dueDate"), filter.month().atDay(1), filter.month().atEndOfMonth()));
      }
      if (filter.category() != null && !filter.category().isBlank()) {
        predicates.add(builder.equal(root.get("category"), filter.category()));
      }
      if (filter.status() != null && !filter.status().isBlank()) {
        if ("OVERDUE".equals(filter.status())) {
          predicates.add(builder.equal(root.get("status"), ExpenseStatus.PENDING));
          predicates.add(builder.lessThan(root.get("dueDate"), LocalDate.now(ZoneOffset.UTC)));
        } else {
          predicates.add(builder.equal(root.get("status"), ExpenseStatus.valueOf(filter.status())));
        }
      }
      if (filter.memberId() != null) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<ExpenseShareEntity> share = subquery.from(ExpenseShareEntity.class);
        subquery.select(share.get("expenseId"));
        subquery.where(builder.equal(share.get("memberId"), filter.memberId()));
        predicates.add(root.get("id").in(subquery));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private Expense withShares(ExpenseEntity entity) {
    return toDomain(entity, shares.findByExpenseIdOrderById(entity.id));
  }

  private ExpenseShareEntity toEntity(ExpenseShare share, ExpenseShareEntity current) {
    ExpenseShareEntity entity = current == null ? new ExpenseShareEntity() : current;
    entity.id = share.id();
    entity.expenseId = share.expenseId();
    entity.memberId = share.memberId();
    entity.amount = share.amount();
    entity.status = share.status();
    entity.settledAt = share.settledAt();
    return entity;
  }

  private Expense toDomain(ExpenseEntity entity, List<ExpenseShareEntity> shareRows) {
    return new Expense(
        entity.id,
        entity.householdId,
        entity.createdByMemberId,
        entity.paidByMemberId,
        entity.title,
        entity.total,
        entity.category,
        entity.dueDate,
        entity.notes,
        entity.splitType,
        entity.currency,
        entity.status,
        entity.createdAt,
        entity.updatedAt,
        entity.cancelledAt,
        entity.version,
        shareRows.stream()
            .map(
                row ->
                    new ExpenseShare(
                        row.id,
                        row.expenseId,
                        row.memberId,
                        row.amount,
                        row.status,
                        row.settledAt,
                        row.version))
            .toList());
  }
}
