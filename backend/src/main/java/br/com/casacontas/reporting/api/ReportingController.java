package br.com.casacontas.reporting.api;

import br.com.casacontas.reporting.application.ReportingService;
import br.com.casacontas.reporting.domain.MonthlyDashboard;
import br.com.casacontas.shared.application.CurrentUser;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/dashboard")
public class ReportingController {

  private final ReportingService service;
  private final CurrentUser currentUser;

  public ReportingController(ReportingService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  MonthlyDashboard dashboard(
      @PathVariable UUID householdId,
      @RequestParam(defaultValue = "#{T(java.time.YearMonth).now().toString()}") YearMonth month) {
    return service.dashboard(currentUser.id(), householdId, month);
  }
}
