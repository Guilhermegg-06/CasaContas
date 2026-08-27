package br.com.casacontas.shared.application;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class CurrentUser {

  public UUID id() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof Jwt jwt)) {
      throw new IllegalStateException("Authenticated JWT principal is required");
    }
    return UUID.fromString(jwt.getSubject());
  }
}
