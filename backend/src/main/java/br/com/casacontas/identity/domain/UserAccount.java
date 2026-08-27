package br.com.casacontas.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(
    UUID id, String name, String email, String passwordHash, Instant createdAt) {}
