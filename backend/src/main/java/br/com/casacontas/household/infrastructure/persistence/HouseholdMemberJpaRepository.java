package br.com.casacontas.household.infrastructure.persistence;

import br.com.casacontas.household.domain.MemberStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface HouseholdMemberJpaRepository extends JpaRepository<HouseholdMemberEntity, UUID> {

  Optional<HouseholdMemberEntity> findByHouseholdIdAndUserId(UUID householdId, UUID userId);

  @Query(
      """
            select member.id as id, member.householdId as householdId, member.userId as userId,
                   user.name as userName, user.email as userEmail, member.role as role,
                   member.status as status, member.joinedAt as joinedAt, member.removedAt as removedAt
            from HouseholdMemberEntity member, UserEntity user
            where member.userId = user.id
              and member.householdId = :householdId
              and member.status = :status
            order by user.name
            """)
  List<MemberView> findViews(
      @Param("householdId") UUID householdId, @Param("status") MemberStatus status);

  interface MemberView {
    UUID getId();

    UUID getHouseholdId();

    UUID getUserId();

    String getUserName();

    String getUserEmail();

    br.com.casacontas.household.domain.MemberRole getRole();

    MemberStatus getStatus();

    Instant getJoinedAt();

    Instant getRemovedAt();
  }
}
