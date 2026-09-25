package br.com.casacontas.household.application;

import br.com.casacontas.shared.application.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HouseholdCalendar {
  private final HouseholdRepository households;
  private final Clock clock;

  public HouseholdCalendar(HouseholdRepository households, Clock clock) {
    this.households = households;
    this.clock = clock;
  }

  public LocalDate today(UUID householdId) {
    var house = households.findHousehold(householdId).orElseThrow(BusinessException::notFound);
    return LocalDate.now(clock.withZone(ZoneId.of(house.timezone())));
  }
}
