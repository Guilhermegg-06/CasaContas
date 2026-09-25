package br.com.casacontas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.casacontas.expense.application.ExpenseService;
import br.com.casacontas.expense.domain.SplitType;
import br.com.casacontas.household.application.HouseholdService;
import br.com.casacontas.identity.application.AuthService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, HouseholdDateIntegrationTest.DateConfig.class})
class HouseholdDateIntegrationTest {
  @Autowired AuthService auth;
  @Autowired HouseholdService houses;
  @Autowired ExpenseService expenses;
  @Autowired MockMvc mvc;

  @Test
  void detailFiltersAndDashboardUseTheSameHouseholdDate() throws Exception {
    var owner = auth.register("Alice", "date@casacontas.test", "Senha-sintetica-123!");
    var house = houses.create(owner.userId(), "Fuso", "America/Fortaleza");
    var member = houses.members(owner.userId(), house.id()).getFirst();
    var dueToday =
        expenses.create(
            owner.userId(),
            new ExpenseService.CreateExpense(
                house.id(),
                "Hoje na casa",
                new BigDecimal("100.00"),
                "Energia",
                LocalDate.of(2029, 12, 31),
                null,
                SplitType.EQUAL,
                List.of(member.id()),
                Map.of()));
    expenses.create(
        owner.userId(),
        new ExpenseService.CreateExpense(
            house.id(),
            "Vencida na casa",
            new BigDecimal("50.00"),
            "Energia",
            LocalDate.of(2029, 12, 30),
            null,
            SplitType.EQUAL,
            List.of(member.id()),
            Map.of()));
    var base = "/api/v1/households/" + house.id();
    var authorization = "Bearer " + owner.accessToken();
    mvc.perform(get(base + "/expenses/" + dueToday.id()).header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));
    mvc.perform(
            get(base + "/expenses?status=OVERDUE&month=2029-12")
                .header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get(base + "/expenses?status=PENDING&month=2029-12")
                .header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get(base + "/dashboard?month=2029-12").header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.overdueExpenses").value(1));
    mvc.perform(get(base + "/expenses?status=UNKNOWN").header("Authorization", authorization))
        .andExpect(status().isBadRequest());
  }

  @TestConfiguration
  static class DateConfig {
    @Bean
    @Primary
    Clock householdTestClock() {
      return Clock.fixed(Instant.parse("2030-01-01T01:00:00Z"), ZoneOffset.UTC);
    }
  }
}
