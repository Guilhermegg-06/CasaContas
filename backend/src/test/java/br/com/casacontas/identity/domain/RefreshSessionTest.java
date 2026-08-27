package br.com.casacontas.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshSessionTest {

  @Test
  void sessionIsActiveOnlyBeforeExpiryAndBeforeRevocation() {
    Instant now = Instant.parse("2026-08-27T00:00:00Z");
    RefreshSession session =
        new RefreshSession(
            UUID.randomUUID(), UUID.randomUUID(), "hash", now.plusSeconds(60), now, null, null);

    assertThat(session.isActiveAt(now)).isTrue();
    assertThat(session.isActiveAt(now.plusSeconds(60))).isFalse();
    assertThat(session.revokeAt(now.plusSeconds(1)).isActiveAt(now.plusSeconds(2))).isFalse();
  }

  @Test
  void rotationRevokesCurrentSessionAndPointsToReplacement() {
    Instant now = Instant.parse("2026-08-27T00:00:00Z");
    UUID replacement = UUID.randomUUID();
    RefreshSession session =
        new RefreshSession(
            UUID.randomUUID(), UUID.randomUUID(), "hash", now.plusSeconds(60), now, null, null);

    RefreshSession rotated = session.rotateTo(replacement, now.plusSeconds(1));

    assertThat(rotated.revokedAt()).isEqualTo(now.plusSeconds(1));
    assertThat(rotated.rotatedTo()).isEqualTo(replacement);
  }
}
