package br.com.casacontas.household.infrastructure.persistence;

import br.com.casacontas.household.application.HouseholdRepository;
import br.com.casacontas.household.domain.Household;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.household.domain.Invitation;
import br.com.casacontas.household.domain.MemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHouseholdAdapter implements HouseholdRepository {

  private final HouseholdJpaRepository households;
  private final HouseholdMemberJpaRepository members;
  private final InvitationJpaRepository invitations;

  public JpaHouseholdAdapter(
      HouseholdJpaRepository households,
      HouseholdMemberJpaRepository members,
      InvitationJpaRepository invitations) {
    this.households = households;
    this.members = members;
    this.invitations = invitations;
  }

  @Override
  public Household saveHousehold(Household household) {
    HouseholdEntity entity = new HouseholdEntity();
    entity.id = household.id();
    entity.name = household.name();
    entity.timezone = household.timezone();
    entity.currency = household.currency();
    entity.createdBy = household.createdBy();
    entity.createdAt = household.createdAt();
    return toDomain(households.save(entity));
  }

  @Override
  public Optional<Household> findHousehold(UUID householdId) {
    return households.findById(householdId).map(this::toDomain);
  }

  @Override
  public List<Household> findActiveHouseholdsByUser(UUID userId) {
    return households.findActiveByUserId(userId).stream().map(this::toDomain).toList();
  }

  @Override
  public HouseholdMember saveMember(HouseholdMember member) {
    HouseholdMemberEntity entity =
        members.findById(member.id()).orElseGet(HouseholdMemberEntity::new);
    entity.id = member.id();
    entity.householdId = member.householdId();
    entity.userId = member.userId();
    entity.role = member.role();
    entity.status = member.status();
    entity.joinedAt = member.joinedAt();
    entity.removedAt = member.removedAt();
    return toDomain(members.save(entity));
  }

  @Override
  public Optional<HouseholdMember> findMember(UUID householdId, UUID userId) {
    return members.findByHouseholdIdAndUserId(householdId, userId).map(this::toDomain);
  }

  @Override
  public Optional<HouseholdMember> findMemberById(UUID memberId) {
    return members.findById(memberId).map(this::toDomain);
  }

  @Override
  public List<HouseholdMember> findActiveMembers(UUID householdId) {
    return members.findViews(householdId, MemberStatus.ACTIVE).stream()
        .map(
            view ->
                new HouseholdMember(
                    view.getId(),
                    view.getHouseholdId(),
                    view.getUserId(),
                    view.getUserName(),
                    view.getUserEmail(),
                    view.getRole(),
                    view.getStatus(),
                    view.getJoinedAt(),
                    view.getRemovedAt()))
        .toList();
  }

  @Override
  public Invitation saveInvitation(Invitation invitation) {
    InvitationEntity entity =
        invitations.findById(invitation.id()).orElseGet(InvitationEntity::new);
    entity.id = invitation.id();
    entity.householdId = invitation.householdId();
    entity.invitedBy = invitation.invitedBy();
    entity.role = invitation.role();
    entity.tokenHash = invitation.tokenHash();
    entity.expiresAt = invitation.expiresAt();
    entity.createdAt = invitation.createdAt();
    entity.usedAt = invitation.usedAt();
    entity.usedBy = invitation.usedBy();
    return toDomain(invitations.save(entity));
  }

  @Override
  public Optional<Invitation> findInvitationForUpdate(String tokenHash) {
    return invitations.findForUpdate(tokenHash).map(this::toDomain);
  }

  private Household toDomain(HouseholdEntity entity) {
    return new Household(
        entity.id,
        entity.name,
        entity.timezone,
        entity.currency,
        entity.createdBy,
        entity.createdAt);
  }

  private HouseholdMember toDomain(HouseholdMemberEntity entity) {
    return new HouseholdMember(
        entity.id,
        entity.householdId,
        entity.userId,
        null,
        null,
        entity.role,
        entity.status,
        entity.joinedAt,
        entity.removedAt);
  }

  private Invitation toDomain(InvitationEntity entity) {
    return new Invitation(
        entity.id,
        entity.householdId,
        entity.invitedBy,
        entity.role,
        entity.tokenHash,
        entity.expiresAt,
        entity.createdAt,
        entity.usedAt,
        entity.usedBy);
  }
}
