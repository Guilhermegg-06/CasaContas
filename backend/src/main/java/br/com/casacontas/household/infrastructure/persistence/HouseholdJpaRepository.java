package br.com.casacontas.household.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface HouseholdJpaRepository extends JpaRepository<HouseholdEntity, UUID> {

  @Query(
      """
            select household from HouseholdEntity household
            join HouseholdMemberEntity member on member.householdId = household.id
            where member.userId = :userId and member.status = br.com.casacontas.household.domain.MemberStatus.ACTIVE
            order by household.name
            """)
  List<HouseholdEntity> findActiveByUserId(@Param("userId") UUID userId);
}
