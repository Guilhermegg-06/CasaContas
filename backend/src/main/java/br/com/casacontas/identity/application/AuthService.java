package br.com.casacontas.identity.application;

import br.com.casacontas.identity.domain.RefreshSession;
import br.com.casacontas.identity.domain.UserAccount;
import br.com.casacontas.shared.application.BusinessException;
import br.com.casacontas.shared.infrastructure.config.SecurityProperties;
import br.com.casacontas.shared.infrastructure.security.OpaqueTokenSupport;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final IdentityRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenIssuer tokenIssuer;
  private final OpaqueTokenSupport opaqueTokens;
  private final SecurityProperties properties;
  private final Clock clock;
  private final String dummyPasswordHash;

  public AuthService(
      IdentityRepository repository,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer tokenIssuer,
      OpaqueTokenSupport opaqueTokens,
      SecurityProperties properties,
      Clock clock) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.tokenIssuer = tokenIssuer;
    this.opaqueTokens = opaqueTokens;
    this.properties = properties;
    this.clock = clock;
    this.dummyPasswordHash = passwordEncoder.encode("constant-time-password-probe");
  }

  @Transactional
  public AuthResult register(String name, String email, String password) {
    String normalizedEmail = normalizeEmail(email);
    if (repository.findUserByEmail(normalizedEmail).isPresent()) {
      throw BusinessException.conflict(
          "ACCOUNT_NOT_AVAILABLE", "Não foi possível criar a conta com os dados informados");
    }
    Instant now = clock.instant();
    UserAccount user =
        repository.saveUser(
            new UserAccount(
                UUID.randomUUID(),
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                now));
    return createSession(user, now);
  }

  @Transactional
  public AuthResult login(String email, String password) {
    String normalizedEmail = normalizeEmail(email);
    UserAccount user = repository.findUserByEmail(normalizedEmail).orElse(null);
    String passwordHash = user == null ? dummyPasswordHash : user.passwordHash();
    if (!passwordEncoder.matches(password, passwordHash) || user == null) {
      throw invalidCredentials();
    }
    return createSession(user, clock.instant());
  }

  @Transactional
  public AuthResult refresh(String refreshToken) {
    Instant now = clock.instant();
    RefreshSession current =
        repository
            .findSessionForUpdate(opaqueTokens.hash(refreshToken))
            .orElseThrow(this::invalidCredentials);
    if (!current.isActiveAt(now)) {
      throw invalidCredentials();
    }
    UserAccount user =
        repository.findUserById(current.userId()).orElseThrow(this::invalidCredentials);
    String rawNextToken = opaqueTokens.generate();
    RefreshSession next =
        new RefreshSession(
            UUID.randomUUID(),
            user.id(),
            opaqueTokens.hash(rawNextToken),
            now.plus(properties.refreshTokenTtl()),
            now,
            null,
            null);
    repository.saveSession(next);
    repository.saveSession(current.rotateTo(next.id(), now));
    return withAccess(user, rawNextToken);
  }

  @Transactional
  public void logout(String refreshToken) {
    Instant now = clock.instant();
    repository
        .findSessionForUpdate(opaqueTokens.hash(refreshToken))
        .filter(session -> session.isActiveAt(now))
        .ifPresent(session -> repository.saveSession(session.revokeAt(now)));
  }

  private AuthResult createSession(UserAccount user, Instant now) {
    String rawToken = opaqueTokens.generate();
    repository.saveSession(
        new RefreshSession(
            UUID.randomUUID(),
            user.id(),
            opaqueTokens.hash(rawToken),
            now.plus(properties.refreshTokenTtl()),
            now,
            null,
            null));
    return withAccess(user, rawToken);
  }

  private AuthResult withAccess(UserAccount user, String refreshToken) {
    AccessTokenIssuer.IssuedAccessToken access = tokenIssuer.issue(user);
    return new AuthResult(
        access.value(),
        access.expiresInSeconds(),
        refreshToken,
        user.id(),
        user.name(),
        user.email());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private BusinessException invalidCredentials() {
    return new BusinessException(
        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos");
  }

  public record AuthResult(
      String accessToken,
      long expiresIn,
      String refreshToken,
      UUID userId,
      String name,
      String email) {}
}
