package br.com.casacontas.identity.infrastructure.security;

import br.com.casacontas.identity.application.AccessTokenIssuer;
import br.com.casacontas.identity.domain.UserAccount;
import br.com.casacontas.shared.infrastructure.config.SecurityProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final SecurityProperties properties;
  private final Clock clock;

  public JwtAccessTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public IssuedAccessToken issue(UserAccount user) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("casacontas")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.id().toString())
            .claim("email", user.email())
            .claim("name", user.name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(value, properties.accessTokenTtl().toSeconds());
  }
}
