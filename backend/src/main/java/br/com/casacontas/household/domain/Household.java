package br.com.casacontas.household.domain;

import java.time.Instant;
import java.util.UUID;

public record Household(
    UUID id, String name, String timezone, String currency, UUID createdBy, Instant createdAt) {}
