package br.com.casacontas.household.application;

import br.com.casacontas.household.domain.Household;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.household.domain.Invitation;
import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.household.domain.MemberStatus;
import br.com.casacontas.shared.application.BusinessException;
import br.com.casacontas.shared.infrastructure.security.OpaqueTokenSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {

  private final HouseholdRepository repository;
  private final HouseholdAccessService access;
  private final OpaqueTokenSupport opaqueTokens;
  private final Clock clock;

  public HouseholdService(
      HouseholdRepository repository,
      HouseholdAccessService access,
      OpaqueTokenSupport opaqueTokens,
      Clock clock) {
    this.repository = repository;
    this.access = access;
    this.opaqueTokens = opaqueTokens;
    this.clock = clock;
  }

  @Transactional
  public Household create(UUID userId, String name, String timezone) {
    String validTimezone = ZoneId.of(timezone).getId();
    Instant now = clock.instant();
    Household household =
        repository.saveHousehold(
            new Household(UUID.randomUUID(), name.trim(), validTimezone, "BRL", userId, now));
    repository.saveMember(
        new HouseholdMember(
            UUID.randomUUID(),
            household.id(),
            userId,
            null,
            null,
            MemberRole.OWNER,
            MemberStatus.ACTIVE,
            now,
            null));
    return household;
  }

  @Transactional(readOnly = true)
  public List<Household> list(UUID userId) {
    return repository.findActiveHouseholdsByUser(userId);
  }

  @Transactional(readOnly = true)
  public List<HouseholdMember> members(UUID userId, UUID householdId) {
    access.requireActiveMember(householdId, userId);
    return repository.findActiveMembers(householdId);
  }

  @Transactional
  public IssuedInvitation invite(
      UUID userId, UUID householdId, MemberRole role, int validityHours) {
    access.requireManager(householdId, userId);
    if (role == MemberRole.OWNER) {
      throw BusinessException.unprocessable(
          "INVALID_ROLE", "Convites não podem transferir a propriedade");
    }
    if (validityHours < 1 || validityHours > 168) {
      throw BusinessException.unprocessable(
          "INVALID_INVITATION_EXPIRY", "A validade deve ficar entre 1 e 168 horas");
    }
    Instant now = clock.instant();
    String rawToken = opaqueTokens.generate();
    Invitation invitation =
        repository.saveInvitation(
            new Invitation(
                UUID.randomUUID(),
                householdId,
                userId,
                role,
                opaqueTokens.hash(rawToken),
                now.plusSeconds(validityHours * 3600L),
                now,
                null,
                null));
    return new IssuedInvitation(
        invitation.id(), rawToken, invitation.role(), invitation.expiresAt());
  }

  @Transactional
  public Household accept(UUID userId, String invitationToken) {
    Instant now = clock.instant();
    Invitation invitation =
        repository
            .findInvitationForUpdate(opaqueTokens.hash(invitationToken))
            .orElseThrow(
                () ->
                    BusinessException.unprocessable(
                        "INVALID_INVITATION", "O convite é inválido ou expirou"));
    if (!invitation.canBeUsedAt(now)) {
      throw BusinessException.unprocessable(
          "INVALID_INVITATION", "O convite é inválido ou expirou");
    }
    if (repository.findMember(invitation.householdId(), userId).isPresent()) {
      throw BusinessException.conflict(
          "MEMBERSHIP_EXISTS", "Este usuário já possui vínculo com a casa");
    }
    repository.saveMember(
        new HouseholdMember(
            UUID.randomUUID(),
            invitation.householdId(),
            userId,
            null,
            null,
            invitation.role(),
            MemberStatus.ACTIVE,
            now,
            null));
    repository.saveInvitation(invitation.use(userId, now));
    return repository
        .findHousehold(invitation.householdId())
        .orElseThrow(BusinessException::notFound);
  }

  @Transactional
  public void removeMember(UUID userId, UUID householdId, UUID memberId) {
    access.requireManager(householdId, userId);
    HouseholdMember target =
        repository
            .findMemberById(memberId)
            .filter(member -> member.householdId().equals(householdId))
            .filter(HouseholdMember::active)
            .orElseThrow(BusinessException::notFound);
    if (target.role() == MemberRole.OWNER) {
      throw BusinessException.unprocessable(
          "OWNER_CANNOT_BE_REMOVED", "O proprietário não pode ser removido da casa");
    }
    repository.saveMember(target.remove(clock.instant()));
  }

  public record IssuedInvitation(UUID id, String token, MemberRole role, Instant expiresAt) {}
}
