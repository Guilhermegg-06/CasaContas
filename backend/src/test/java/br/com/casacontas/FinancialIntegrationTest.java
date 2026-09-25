package br.com.casacontas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.casacontas.expense.application.ExpenseService;
import br.com.casacontas.expense.domain.Expense;
import br.com.casacontas.expense.domain.SplitType;
import br.com.casacontas.household.application.HouseholdService;
import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.identity.application.AuthService;
import br.com.casacontas.settlement.application.SettlementService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FinancialIntegrationTest {
  @Autowired AuthService auth;
  @Autowired HouseholdService houses;
  @Autowired ExpenseService expenses;
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired PlatformTransactionManager transactions;
  @Autowired SettlementService settlements;

  @Test
  void editWaitsForPaymentAndRejectsChangesAfterItCommits() throws Exception {
    Seed seed = seed("edit-race");
    var result =
        afterConcurrentPayment(
            seed,
            () ->
                mvc.perform(
                        put(base(seed))
                            .header("Authorization", "Bearer " + seed.owner.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request(seed, "60.00", null)))
                    .andReturn());
    assertThat(result.getResponse().getStatus()).isEqualTo(409);
    assertThat(expenses.detail(seed.owner.userId(), seed.house, seed.expense.id()).total())
        .isEqualByComparingTo("100.00");
    assertThat(count("settlements", seed.expense.id())).isEqualTo(1);
  }

  @Test
  void cancellationWaitsForPaymentAndPreservesItsShares() throws Exception {
    Seed seed = seed("cancel-race");
    var result =
        afterConcurrentPayment(
            seed,
            () ->
                mvc.perform(
                        delete(base(seed))
                            .header("Authorization", "Bearer " + seed.owner.accessToken()))
                    .andReturn());
    assertThat(result.getResponse().getStatus()).isEqualTo(204);
    var expense = expenses.detail(seed.owner.userId(), seed.house, seed.expense.id());
    assertThat(expense.status().name()).isEqualTo("CANCELLED");
    assertThat(expense.shares().stream().filter(share -> share.status().name().equals("SETTLED")))
        .hasSize(1);
    assertThat(count("settlements", seed.expense.id())).isEqualTo(1);
  }

  private MvcResult afterConcurrentPayment(Seed seed, Callable<MvcResult> request)
      throws Exception {
    try (var executor = Executors.newSingleThreadExecutor()) {
      var waiting =
          new TransactionTemplate(transactions)
              .execute(
                  transaction -> {
                    jdbc.queryForList(
                        "select id from expenses where id = ? for update", seed.expense.id());
                    var future = executor.submit(request);
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                    int blocked;
                    do {
                      jdbc.execute("select pg_stat_clear_snapshot()");
                      blocked =
                          jdbc.queryForObject(
                              "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and pid <> pg_backend_pid()",
                              Integer.class);
                      if (blocked == 1) break;
                      try {
                        Thread.sleep(50);
                      } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                      }
                    } while (System.nanoTime() < deadline);
                    assertThat(blocked)
                        .as("requisição aguarda a transação financeira")
                        .isEqualTo(1);
                    settlements.settleShare(
                        seed.owner.userId(),
                        seed.house,
                        seed.expense.id(),
                        seed.expense.shares().get(1).id(),
                        "payment-before-edit");
                    return future;
                  });
      return waiting.get(20, TimeUnit.SECONDS);
    }
  }

  @Test
  void simultaneousRetriesReturnTheSameSettlementAndSingleAuditEvent() throws Exception {
    Seed seed = seed("retry");
    var responses =
        concurrent(
            seed.expense.id(), () -> settle(seed, "same-key"), () -> settle(seed, "same-key"));
    assertThat(responses)
        .extracting(result -> result.getResponse().getStatus())
        .containsExactly(200, 200);
    assertThat(id(responses.get(0))).isEqualTo(id(responses.get(1)));
    assertThat(count("settlements", seed.expense.id())).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_events where expense_id = ? and event_type = 'SHARE_SETTLED'",
                Integer.class,
                seed.expense.id()))
        .isEqualTo(1);
  }

  @Test
  void differentKeysCannotSettleTheSameShareTwice() throws Exception {
    Seed seed = seed("different");
    var responses =
        concurrent(seed.expense.id(), () -> settle(seed, "first"), () -> settle(seed, "second"));
    assertThat(responses)
        .extracting(result -> result.getResponse().getStatus())
        .containsExactlyInAnyOrder(200, 422);
    assertThat(count("settlements", seed.expense.id())).isEqualTo(1);
  }

  @Test
  void simultaneousPrimaryPaymentRetriesReturnTheSamePayment() throws Exception {
    Seed seed = seed("primary");
    Callable<MvcResult> payment =
        () ->
            mvc.perform(
                    post(base(seed) + "/primary-payment")
                        .header("Authorization", "Bearer " + seed.owner.accessToken())
                        .header("Idempotency-Key", "primary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payerMemberId\":\"" + seed.members.get(0) + "\"}"))
                .andReturn();
    var responses = concurrent(seed.expense.id(), payment, payment);
    assertThat(responses)
        .extracting(result -> result.getResponse().getStatus())
        .containsExactly(200, 200);
    assertThat(id(responses.get(0))).isEqualTo(id(responses.get(1)));
    assertThat(count("settlements", seed.expense.id())).isEqualTo(1);
  }

  @Test
  void failedPaymentDuringCreationLeavesNoExpenseSharesOrAudit() throws Exception {
    Seed seed = seed("atomic");
    int before =
        jdbc.queryForObject(
            "select count(*) from expenses where household_id = ?", Integer.class, seed.house);
    String request =
        request(seed, "50.00", UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    mvc.perform(
            post("/api/v1/households/" + seed.house + "/expenses")
                .header("Authorization", "Bearer " + seed.owner.accessToken())
                .header("Idempotency-Key", "invalid-payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from expenses where household_id = ?", Integer.class, seed.house))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_events where household_id = ?",
                Integer.class,
                seed.house))
        .isEqualTo(1);
  }

  @Test
  void editingKeepsPreviousFinancialSharesInsteadOfDeletingThem() throws Exception {
    Seed seed = seed("history");
    List<UUID> original = seed.expense.shares().stream().map(share -> share.id()).toList();
    mvc.perform(
            put(base(seed))
                .header("Authorization", "Bearer " + seed.owner.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(seed, "60.00", null)))
        .andExpect(status().isOk());
    for (UUID share : original) {
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from expense_shares where id = ?", Integer.class, share))
          .isEqualTo(1);
    }
    String detail =
        mvc.perform(get(base(seed)).header("Authorization", "Bearer " + seed.owner.accessToken()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<Integer>read(detail, "$.shares.length()")).isEqualTo(2);
    assertThat(JsonPath.<Double>read(detail, "$.total")).isEqualTo(60.0);
  }

  @Test
  void invalidCustomShareAndOversizedMoneyAreRejectedBeforePersistence() throws Exception {
    Seed seed = seed("validation");
    String missing = request(seed, "50.00", null).replace("EQUAL", "CUSTOM");
    int missingStatus =
        mvc.perform(
                post("/api/v1/households/" + seed.house + "/expenses")
                    .header("Authorization", "Bearer " + seed.owner.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(missing))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(missingStatus).isIn(400, 422);
    mvc.perform(
            post("/api/v1/households/" + seed.house + "/expenses")
                .header("Authorization", "Bearer " + seed.owner.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(seed, "100000000000000000.00", null)))
        .andExpect(status().isBadRequest());
  }

  private MvcResult settle(Seed seed, String key) throws Exception {
    return mvc.perform(
            post(base(seed) + "/shares/" + seed.expense.shares().get(1).id() + "/settlements")
                .header("Authorization", "Bearer " + seed.owner.accessToken())
                .header("Idempotency-Key", key))
        .andReturn();
  }

  private List<MvcResult> concurrent(
      UUID expense, Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
    try (Connection lock = dataSource.getConnection();
        var executor = Executors.newFixedThreadPool(2)) {
      lock.setAutoCommit(false);
      int blocker;
      try (var statement = lock.createStatement();
          var result = statement.executeQuery("select pg_backend_pid()")) {
        result.next();
        blocker = result.getInt(1);
      }
      try (var statement =
          lock.prepareStatement("select id from expenses where id = ? for update")) {
        statement.setObject(1, expense);
        statement.executeQuery().close();
      }
      var a = executor.submit(first);
      var b = executor.submit(second);
      try {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        int waiting;
        do {
          waiting =
              jdbc.queryForObject(
                  "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and query like '%expenses%' and pid <> ?",
                  Integer.class, blocker);
          if (waiting >= 2) break;
          Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        assertThat(waiting)
            .as("duas transações realmente aguardam o bloqueio PostgreSQL")
            .isEqualTo(2);
      } finally {
        lock.commit();
      }
      return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
    }
  }

  private Seed seed(String name) {
    var owner = auth.register("Alice", name + "-alice@casacontas.test", "Senha-sintetica-123!");
    var resident = auth.register("Bruno", name + "-bruno@casacontas.test", "Senha-sintetica-123!");
    UUID house = houses.create(owner.userId(), "Casa " + name, "America/Fortaleza").id();
    houses.accept(
        resident.userId(), houses.invite(owner.userId(), house, MemberRole.MEMBER, 24).token());
    var members =
        houses.members(owner.userId(), house).stream().map(member -> member.id()).toList();
    var expense =
        expenses.create(
            owner.userId(),
            new ExpenseService.CreateExpense(
                house,
                "Energia",
                new BigDecimal("100.00"),
                "Energia",
                LocalDate.of(2026, 9, 30),
                null,
                SplitType.EQUAL,
                members,
                Map.of()));
    return new Seed(owner, house, members, expense);
  }

  private String request(Seed seed, String total, UUID payer) {
    return "{\"title\":\"Energia\",\"total\":\""
        + total
        + "\",\"category\":\"Energia\",\"dueDate\":\"2026-09-30\",\"splitType\":\"EQUAL\",\"participants\":[{\"memberId\":\""
        + seed.members.get(0)
        + "\"},{\"memberId\":\""
        + seed.members.get(1)
        + "\"}],\"paidByMemberId\":"
        + (payer == null ? "null" : "\"" + payer + "\"")
        + "}";
  }

  private String base(Seed seed) {
    return "/api/v1/households/" + seed.house + "/expenses/" + seed.expense.id();
  }

  private String id(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private int count(String table, UUID expense) {
    return jdbc.queryForObject(
        "select count(*) from " + table + " where expense_id = ?", Integer.class, expense);
  }

  private record Seed(
      AuthService.AuthResult owner, UUID house, List<UUID> members, Expense expense) {}
}
