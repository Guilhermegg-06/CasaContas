package br.com.casacontas.household.api;

import br.com.casacontas.household.application.HouseholdAccessService;
import br.com.casacontas.household.application.HouseholdService;
import br.com.casacontas.household.domain.Household;
import br.com.casacontas.household.domain.HouseholdMember;
import br.com.casacontas.household.domain.MemberRole;
import br.com.casacontas.shared.application.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HouseholdController {

  private final HouseholdService service;
  private final HouseholdAccessService access;
  private final CurrentUser currentUser;

  public HouseholdController(
      HouseholdService service, HouseholdAccessService access, CurrentUser currentUser) {
    this.service = service;
    this.access = access;
    this.currentUser = currentUser;
  }

  @PostMapping("/households")
  ResponseEntity<HouseholdResponse> create(@Valid @RequestBody CreateHouseholdRequest request) {
    Household household = service.create(currentUser.id(), request.name(), request.timezone());
    HouseholdResponse response = HouseholdResponse.from(household, MemberRole.OWNER);
    return ResponseEntity.created(URI.create("/api/v1/households/" + household.id()))
        .body(response);
  }

  @GetMapping("/households")
  List<HouseholdResponse> list() {
    UUID userId = currentUser.id();
    return service.list(userId).stream()
        .map(
            household ->
                HouseholdResponse.from(
                    household, access.requireActiveMember(household.id(), userId).role()))
        .toList();
  }

  @GetMapping("/households/{householdId}/members")
  List<MemberResponse> members(@PathVariable UUID householdId) {
    return service.members(currentUser.id(), householdId).stream()
        .map(MemberResponse::from)
        .toList();
  }

  @PostMapping("/households/{householdId}/invitations")
  ResponseEntity<InvitationResponse> invite(
      @PathVariable UUID householdId, @Valid @RequestBody InvitationRequest request) {
    HouseholdService.IssuedInvitation invitation =
        service.invite(currentUser.id(), householdId, request.role(), request.validityHours());
    InvitationResponse response =
        new InvitationResponse(
            invitation.id(), invitation.token(), invitation.role(), invitation.expiresAt());
    return ResponseEntity.created(
            URI.create("/api/v1/households/" + householdId + "/invitations/" + invitation.id()))
        .body(response);
  }

  @PostMapping("/invitations/accept")
  HouseholdResponse accept(@Valid @RequestBody AcceptInvitationRequest request) {
    Household household = service.accept(currentUser.id(), request.token());
    MemberRole role = access.requireActiveMember(household.id(), currentUser.id()).role();
    return HouseholdResponse.from(household, role);
  }

  @DeleteMapping("/households/{householdId}/members/{memberId}")
  ResponseEntity<Void> removeMember(@PathVariable UUID householdId, @PathVariable UUID memberId) {
    service.removeMember(currentUser.id(), householdId, memberId);
    return ResponseEntity.noContent().build();
  }

  record CreateHouseholdRequest(
      @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 64) String timezone) {}

  record InvitationRequest(@NotNull MemberRole role, @Min(1) @Max(168) int validityHours) {}

  record AcceptInvitationRequest(@NotBlank @Size(max = 200) String token) {}

  record HouseholdResponse(
      UUID id, String name, String timezone, String currency, MemberRole role) {
    static HouseholdResponse from(Household household, MemberRole role) {
      return new HouseholdResponse(
          household.id(), household.name(), household.timezone(), household.currency(), role);
    }
  }

  record MemberResponse(
      UUID id, UUID userId, String name, String email, MemberRole role, Instant joinedAt) {
    static MemberResponse from(HouseholdMember member) {
      return new MemberResponse(
          member.id(),
          member.userId(),
          member.userName(),
          member.userEmail(),
          member.role(),
          member.joinedAt());
    }
  }

  record InvitationResponse(UUID id, String token, MemberRole role, Instant expiresAt) {}
}
