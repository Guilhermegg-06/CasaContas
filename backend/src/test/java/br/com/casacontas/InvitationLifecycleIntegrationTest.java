package br.com.casacontas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.application.HouseholdService;
import br.com.casacontas.household.domain.Invitation;
import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.identity.application.AuthService;
import br.com.casacontas.shared.infrastructure.security.OpaqueTokenSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InvitationLifecycleIntegrationTest {
  @Autowired AuthService auth;
  @Autowired HouseholdService houses;
  @Autowired HouseholdRepository repository;
  @Autowired OpaqueTokenSupport tokens;
  @Autowired PlatformTransactionManager transactions;
  @Autowired MockMvc mvc;

  @Test
  void concurrentAcceptanceCreatesOnlyOneMembershipAndPersistsTheWinner() throws Exception {
    var owner = auth.register("Dona", "invite-race-owner@casacontas.test", "Senha-sintetica-123!");
    var alice = auth.register("Alice", "invite-race-alice@casacontas.test", "Senha-sintetica-123!");
    var bruno = auth.register("Bruno", "invite-race-bruno@casacontas.test", "Senha-sintetica-123!");
    UUID house = houses.create(owner.userId(), "Casa concorrente", "America/Fortaleza").id();
    String token = houses.invite(owner.userId(), house, MemberRole.MEMBER, 24).token();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Attempt> outcomes;
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> attempt(alice, token, ready, start));
      var second = executor.submit(() -> attempt(bruno, token, ready, start));
      assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      outcomes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }
    assertThat(outcomes).extracting(Attempt::status).containsExactlyInAnyOrder(200, 422);
    UUID winner =
        outcomes.stream()
            .filter(outcome -> outcome.status() == 200)
            .findFirst()
            .orElseThrow()
            .userId();
    var stored = invitation(token);
    assertThat(stored.usedBy()).isEqualTo(winner);
    assertThat(stored.usedAt()).isNotNull();
    assertThat(houses.members(owner.userId(), house))
        .extracting(member -> member.userId())
        .containsExactlyInAnyOrder(owner.userId(), winner);
    mvc.perform(
            post("/api/v1/invitations/accept")
                .header("Authorization", "Bearer " + alice.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    assertThat(houses.members(owner.userId(), house)).hasSize(2);
  }

  @Test
  void expiredInvitationIsRejectedWithoutConsumptionOrMembershipChanges() throws Exception {
    var owner =
        auth.register("Dona", "invite-expired-owner@casacontas.test", "Senha-sintetica-123!");
    var resident =
        auth.register("Carol", "invite-expired-resident@casacontas.test", "Senha-sintetica-123!");
    UUID house = houses.create(owner.userId(), "Casa expiração", "America/Fortaleza").id();
    String token = "convite-sintetico-expirado-exclusivo-do-teste";
    Instant created = Instant.parse("2000-01-01T00:00:00Z");
    repository.saveInvitation(
        new Invitation(
            UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8)),
            house,
            owner.userId(),
            MemberRole.MEMBER,
            tokens.hash(token),
            created.plusSeconds(3600),
            created,
            null,
            null));
    mvc.perform(
            post("/api/v1/invitations/accept")
                .header("Authorization", "Bearer " + resident.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    assertThat(houses.members(owner.userId(), house))
        .extracting(member -> member.userId())
        .containsExactly(owner.userId());
    var stored = invitation(token);
    assertThat(stored.usedAt()).isNull();
    assertThat(stored.usedBy()).isNull();
    assertThat(stored.expiresAt()).isEqualTo(created.plusSeconds(3600));
  }

  private Attempt attempt(
      AuthService.AuthResult user, String token, CountDownLatch ready, CountDownLatch start)
      throws Exception {
    ready.countDown();
    assertThat(start.await(30, TimeUnit.SECONDS)).isTrue();
    int code =
        mvc.perform(
                post("/api/v1/invitations/accept")
                    .header("Authorization", "Bearer " + user.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"%s\"}".formatted(token)))
            .andReturn()
            .getResponse()
            .getStatus();
    return new Attempt(user.userId(), code);
  }

  private Invitation invitation(String token) {
    return new TransactionTemplate(transactions)
        .execute(status -> repository.findInvitationForUpdate(tokens.hash(token)).orElseThrow());
  }

  private record Attempt(UUID userId, int status) {}
}
