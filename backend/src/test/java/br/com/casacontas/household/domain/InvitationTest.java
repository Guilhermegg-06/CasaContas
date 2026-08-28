package br.com.casacontas.household.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationTest {

  @Test
  void invitationCanBeUsedExactlyOnceBeforeExpiry() {
    Instant now = Instant.parse("2026-08-27T00:00:00Z");
    Invitation invitation =
        new Invitation(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            MemberRole.MEMBER,
            "hash",
            now.plusSeconds(60),
            now,
            null,
            null);

    assertThat(invitation.canBeUsedAt(now)).isTrue();
    assertThat(invitation.canBeUsedAt(now.plusSeconds(60))).isFalse();
    assertThat(
            invitation.use(UUID.randomUUID(), now.plusSeconds(1)).canBeUsedAt(now.plusSeconds(2)))
        .isFalse();
  }
}
