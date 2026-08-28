package br.com.casacontas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CriticalFlowsIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void completesRegistrationInvitationExpenseSettlementAndIsolationFlows() throws Exception {
    Session alice = register("Alice Moura", "alice@casacontas.test");
    Session bruno = register("Bruno Lima", "bruno@casacontas.test");

    String householdBody =
        mockMvc
            .perform(
                post("/api/v1/households")
                    .header("Authorization", bearer(alice.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {"name":"Apartamento Pajuçara","timezone":"America/Maceio"}
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("OWNER"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String householdId = JsonPath.read(householdBody, "$.id");

    String invitationBody =
        mockMvc
            .perform(
                post("/api/v1/households/{id}/invitations", householdId)
                    .header("Authorization", bearer(alice.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {"role":"MEMBER","validityHours":24}
                                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String invitationToken = JsonPath.read(invitationBody, "$.token");

    mockMvc
        .perform(
            post("/api/v1/invitations/accept")
                .header("Authorization", bearer(bruno.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(invitationToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(householdId));

    String membersBody =
        mockMvc
            .perform(
                get("/api/v1/households/{id}/members", householdId)
                    .header("Authorization", bearer(alice.accessToken())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String aliceMemberId = memberIdByEmail(membersBody, "alice@casacontas.test");
    String brunoMemberId = memberIdByEmail(membersBody, "bruno@casacontas.test");

    String expenseBody =
        mockMvc
            .perform(
                post("/api/v1/households/{id}/expenses", householdId)
                    .header("Authorization", bearer(alice.accessToken()))
                    .header("Idempotency-Key", "create-energy-2026-08")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "title":"Energia de agosto",
                                  "total":100.00,
                                  "category":"Moradia",
                                  "dueDate":"2026-08-30",
                                  "splitType":"EQUAL",
                                  "participants":[
                                    {"memberId":"%s"},
                                    {"memberId":"%s"}
                                  ],
                                  "paidByMemberId":"%s"
                                }
                                """
                            .formatted(aliceMemberId, brunoMemberId, aliceMemberId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(100.00))
            .andExpect(jsonPath("$.shares.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String expenseId = JsonPath.read(expenseBody, "$.id");
    String brunoShareId = shareIdByMember(expenseBody, brunoMemberId);

    String settlementBody =
        mockMvc
            .perform(
                post(
                        "/api/v1/households/{householdId}/expenses/{expenseId}/shares/{shareId}/settlements",
                        householdId,
                        expenseId,
                        brunoShareId)
                    .header("Authorization", bearer(bruno.accessToken()))
                    .header("Idempotency-Key", "bruno-energy-refund"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("REIMBURSEMENT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String settlementId = JsonPath.read(settlementBody, "$.id");

    mockMvc
        .perform(
            post(
                    "/api/v1/households/{householdId}/expenses/{expenseId}/shares/{shareId}/settlements",
                    householdId,
                    expenseId,
                    brunoShareId)
                .header("Authorization", bearer(bruno.accessToken()))
                .header("Idempotency-Key", "bruno-energy-refund"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(settlementId));

    mockMvc
        .perform(
            get("/api/v1/households/{id}/dashboard", householdId)
                .param("month", "2026-08")
                .header("Authorization", bearer(bruno.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.householdTotal").value(100.00))
        .andExpect(jsonPath("$.pendingTotal").value(0.00));

    String privateHouse =
        mockMvc
            .perform(
                post("/api/v1/households")
                    .header("Authorization", bearer(alice.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Casa privada\",\"timezone\":\"America/Fortaleza\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String privateHouseId = JsonPath.read(privateHouse, "$.id");
    mockMvc
        .perform(
            get("/api/v1/households/{id}/expenses", privateHouseId)
                .header("Authorization", bearer(bruno.accessToken())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

    String refreshedBody =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"%s\"}".formatted(alice.refreshToken())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<String>read(refreshedBody, "$.refreshToken"))
        .isNotEqualTo(alice.refreshToken());
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(alice.refreshToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  private Session register(String name, String email) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {"name":"%s","email":"%s","password":"Senha forte 123"}
                                """
                            .formatted(name, email)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new Session(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
  }

  private String memberIdByEmail(String json, String email) {
    List<String> ids = JsonPath.read(json, "$[?(@.email == '%s')].id".formatted(email));
    return ids.getFirst();
  }

  private String shareIdByMember(String json, String memberId) {
    List<String> ids =
        JsonPath.read(json, "$.shares[?(@.memberId == '%s')].id".formatted(memberId));
    return ids.getFirst();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record Session(String accessToken, String refreshToken) {}
}
