package br.com.casacontas.reporting.application;

import br.com.casacontas.household.application.HouseholdAccessService;
import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.domain.Household;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.reporting.domain.MonthlyDashboard;
import br.com.casacontas.shared.application.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

  private final ReportingRepository reporting;
  private final HouseholdRepository households;
  private final HouseholdAccessService access;
  private final Clock clock;

  public ReportingService(
      ReportingRepository reporting,
      HouseholdRepository households,
      HouseholdAccessService access,
      Clock clock) {
    this.reporting = reporting;
    this.households = households;
    this.access = access;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public MonthlyDashboard dashboard(UUID userId, UUID householdId, YearMonth month) {
    HouseholdMember member = access.requireActiveMember(householdId, userId);
    Household household =
        households.findHousehold(householdId).orElseThrow(BusinessException::notFound);
    LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(household.timezone())));
    return reporting.dashboard(householdId, member.id(), month, today);
  }
}
