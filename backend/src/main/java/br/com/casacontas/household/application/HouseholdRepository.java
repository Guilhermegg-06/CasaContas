package br.com.casacontas.household.application;

import br.com.casacontas.household.domain.Household;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.household.domain.Invitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository {

  Household saveHousehold(Household household);

  Optional<Household> findHousehold(UUID householdId);

  List<Household> findActiveHouseholdsByUser(UUID userId);

  HouseholdMember saveMember(HouseholdMember member);

  Optional<HouseholdMember> findMember(UUID householdId, UUID userId);

  Optional<HouseholdMember> findMemberById(UUID memberId);

  List<HouseholdMember> findActiveMembers(UUID householdId);

  Invitation saveInvitation(Invitation invitation);

  Optional<Invitation> findInvitationForUpdate(String tokenHash);
}
