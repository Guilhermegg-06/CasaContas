package br.com.casacontas.identity.application;

import br.com.casacontas.identity.domain.RefreshSession;
import br.com.casacontas.identity.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {

  Optional<UserAccount> findUserByEmail(String email);

  Optional<UserAccount> findUserById(UUID id);

  UserAccount saveUser(UserAccount user);

  Optional<RefreshSession> findSessionForUpdate(String tokenHash);

  RefreshSession saveSession(RefreshSession session);
}
