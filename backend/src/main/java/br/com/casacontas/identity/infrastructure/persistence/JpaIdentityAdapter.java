package br.com.casacontas.identity.infrastructure.persistence;

import br.com.casacontas.identity.application.IdentityRepository;
import br.com.casacontas.identity.domain.RefreshSession;
import br.com.casacontas.identity.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaIdentityAdapter implements IdentityRepository {

  private final UserJpaRepository users;
  private final RefreshSessionJpaRepository sessions;

  public JpaIdentityAdapter(UserJpaRepository users, RefreshSessionJpaRepository sessions) {
    this.users = users;
    this.sessions = sessions;
  }

  @Override
  public Optional<UserAccount> findUserByEmail(String email) {
    return users.findByEmail(email).map(this::toDomain);
  }

  @Override
  public Optional<UserAccount> findUserById(UUID id) {
    return users.findById(id).map(this::toDomain);
  }

  @Override
  public UserAccount saveUser(UserAccount user) {
    UserEntity entity = new UserEntity();
    entity.id = user.id();
    entity.name = user.name();
    entity.email = user.email();
    entity.passwordHash = user.passwordHash();
    entity.createdAt = user.createdAt();
    return toDomain(users.save(entity));
  }

  @Override
  public Optional<RefreshSession> findSessionForUpdate(String tokenHash) {
    return sessions.findForUpdate(tokenHash).map(this::toDomain);
  }

  @Override
  public RefreshSession saveSession(RefreshSession session) {
    RefreshSessionEntity entity =
        sessions.findById(session.id()).orElseGet(RefreshSessionEntity::new);
    entity.id = session.id();
    entity.userId = session.userId();
    entity.tokenHash = session.tokenHash();
    entity.expiresAt = session.expiresAt();
    entity.createdAt = session.createdAt();
    entity.revokedAt = session.revokedAt();
    entity.rotatedTo = session.rotatedTo();
    return toDomain(sessions.save(entity));
  }

  private UserAccount toDomain(UserEntity entity) {
    return new UserAccount(
        entity.id, entity.name, entity.email, entity.passwordHash, entity.createdAt);
  }

  private RefreshSession toDomain(RefreshSessionEntity entity) {
    return new RefreshSession(
        entity.id,
        entity.userId,
        entity.tokenHash,
        entity.expiresAt,
        entity.createdAt,
        entity.revokedAt,
        entity.rotatedTo);
  }
}
