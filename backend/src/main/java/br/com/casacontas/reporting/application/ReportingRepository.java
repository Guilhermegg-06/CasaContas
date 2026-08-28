package br.com.casacontas.reporting.application;

import br.com.casacontas.reporting.domain.MonthlyDashboard;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public interface ReportingRepository {

  MonthlyDashboard dashboard(UUID householdId, UUID memberId, YearMonth month, LocalDate today);
}
