package br.com.casacontas.household.application;

import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.shared.application.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HouseholdAccessService {

  private final HouseholdRepository repository;

  public HouseholdAccessService(HouseholdRepository repository) {
    this.repository = repository;
  }

  public HouseholdMember requireActiveMember(UUID householdId, UUID userId) {
    return repository
        .findMember(householdId, userId)
        .filter(HouseholdMember::active)
        .orElseThrow(BusinessException::notFound);
  }

  public HouseholdMember requireManager(UUID householdId, UUID userId) {
    HouseholdMember member = requireActiveMember(householdId, userId);
    if (!member.role().canManageMembers()) {
      throw BusinessException.forbidden(
          "Somente proprietário ou administrador pode gerenciar moradores");
    }
    return member;
  }

  public HouseholdMember requireOwner(UUID householdId, UUID userId) {
    HouseholdMember member = requireActiveMember(householdId, userId);
    if (member.role() != MemberRole.OWNER) {
      throw BusinessException.forbidden("Somente o proprietário pode realizar esta ação");
    }
    return member;
  }
}
