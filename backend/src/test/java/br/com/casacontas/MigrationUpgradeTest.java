package br.com.casacontas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class MigrationUpgradeTest {
  @Test
  void upgradesPopulatedV1WithoutLosingAmountsHistoryOrConstraints() {
    try (var database = new PostgreSQLContainer("postgres:17.6-alpine")) {
      database.start();
      var source =
          new DriverManagerDataSource(
              database.getJdbcUrl(), database.getUsername(), database.getPassword());
      var jdbc = new JdbcTemplate(source);
      Flyway.configure().dataSource(source).target("1").load().migrate();
      jdbc.execute(
          """
          INSERT INTO users VALUES ('10000000-0000-0000-0000-000000000001', 'Sintético', 'migration@casacontas.test', 'synthetic-not-a-login', '2026-09-01Z');
          INSERT INTO households VALUES ('20000000-0000-0000-0000-000000000001', 'Migração', 'America/Fortaleza', 'BRL', '10000000-0000-0000-0000-000000000001', '2026-09-01Z');
          INSERT INTO household_members VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'OWNER', 'ACTIVE', '2026-09-01Z', NULL);
          INSERT INTO expenses (id, household_id, created_by_member_id, title, total, category, due_date, split_type, status, created_at, updated_at)
            VALUES ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Energia', 100.00, 'Energia', '2026-09-30', 'EQUAL', 'SETTLED', '2026-09-01Z', '2026-09-01Z');
          INSERT INTO expense_shares (id, expense_id, member_id, amount, status) VALUES ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 100.00, 'SETTLED');
          INSERT INTO settlements (id, expense_id, share_id, household_id, actor_user_id, payer_member_id, amount, type, occurred_at)
            VALUES ('60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 100.00, 'SHARE_PAYMENT', '2026-09-01Z');
          INSERT INTO audit_events VALUES ('70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'SHARE_SETTLED', '{}', '2026-09-01Z');
          """);
      var before = jdbc.queryForList("select id, amount, type from settlements");
      var audit = jdbc.queryForList("select * from audit_events");
      var flyway = Flyway.configure().dataSource(source).load();
      assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
      flyway.validate();
      assertThat(flyway.migrate().migrationsExecuted).isZero();
      assertThat(jdbc.queryForList("select id, amount, type from settlements")).isEqualTo(before);
      assertThat(jdbc.queryForList("select * from audit_events")).isEqualTo(audit);
      assertThat(jdbc.queryForObject("select active from expense_shares", Boolean.class)).isTrue();
      assertThatThrownBy(
              () ->
                  jdbc.execute(
                      """
          INSERT INTO settlements SELECT '60000000-0000-0000-0000-000000000002', expense_id, share_id, household_id, actor_user_id, payer_member_id, recipient_member_id, amount, type, reverses_settlement_id, occurred_at FROM settlements
          """))
          .isInstanceOf(DuplicateKeyException.class);
      assertThatThrownBy(
              () ->
                  jdbc.execute(
                      """
          INSERT INTO expense_shares SELECT '50000000-0000-0000-0000-000000000002', expense_id, member_id, amount, status, settled_at, version, active FROM expense_shares
          """))
          .isInstanceOf(DuplicateKeyException.class);
    }
  }
}
