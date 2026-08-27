package br.com.casacontas.identity.application;

import br.com.casacontas.identity.domain.UserAccount;

public interface AccessTokenIssuer {

  IssuedAccessToken issue(UserAccount user);

  record IssuedAccessToken(String value, long expiresInSeconds) {}
}
