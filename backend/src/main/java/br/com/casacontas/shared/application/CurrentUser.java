package br.com.casacontas.shared.application;

import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class CurrentUser {

  public UUID id() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (!(principal instanceof Jwt jwt)) {
      throw new IllegalStateException("Authenticated JWT principal is required");
    }
    return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
  }
}
