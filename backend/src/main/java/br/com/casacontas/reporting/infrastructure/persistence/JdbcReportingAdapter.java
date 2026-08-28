package br.com.casacontas.reporting.infrastructure.persistence;

import br.com.casacontas.reporting.application.ReportingRepository;
import br.com.casacontas.reporting.domain.MonthlyDashboard;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReportingAdapter implements ReportingRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcReportingAdapter(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public MonthlyDashboard dashboard(
      UUID householdId, UUID memberId, YearMonth month, LocalDate today) {
    Map<String, Object> parameters =
        Map.of(
            "householdId", householdId,
            "memberId", memberId,
            "start", month.atDay(1),
            "end", month.atEndOfMonth(),
            "today", today);
    String sql =
        """
                WITH month_expenses AS (
                  SELECT *
                  FROM expenses
                  WHERE household_id = :householdId
                    AND due_date BETWEEN :start AND :end
                )
                SELECT
                  COALESCE((SELECT SUM(total) FROM month_expenses WHERE status <> 'CANCELLED'), 0) AS household_total,
                  COALESCE(SUM(es.amount) FILTER (WHERE e.status <> 'CANCELLED' AND es.status IN ('COVERED','SETTLED')), 0) AS paid_total,
                  COALESCE(SUM(es.amount) FILTER (WHERE e.status <> 'CANCELLED' AND es.status = 'PENDING'), 0) AS pending_total,
                  COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'PENDING' AND e.due_date < :today) AS overdue_expenses,
                  COALESCE(SUM(es.amount) FILTER (WHERE e.status <> 'CANCELLED' AND es.member_id = :memberId AND es.status = 'PENDING'), 0) AS i_owe,
                  COALESCE(SUM(es.amount) FILTER (WHERE e.status <> 'CANCELLED' AND es.member_id = :memberId AND es.status IN ('COVERED','SETTLED')), 0) AS i_paid,
                  COALESCE(SUM(es.amount) FILTER (WHERE e.status <> 'CANCELLED' AND e.paid_by_member_id = :memberId AND es.member_id <> :memberId AND es.status = 'PENDING'), 0) AS i_receive
                FROM month_expenses e
                LEFT JOIN expense_shares es ON es.expense_id = e.id
                """;
    return jdbc.queryForObject(
        sql,
        parameters,
        (result, row) ->
            new MonthlyDashboard(
                month,
                result.getBigDecimal("household_total"),
                result.getBigDecimal("paid_total"),
                result.getBigDecimal("pending_total"),
                result.getLong("overdue_expenses"),
                result.getBigDecimal("i_owe"),
                result.getBigDecimal("i_paid"),
                result.getBigDecimal("i_receive")));
  }
}
