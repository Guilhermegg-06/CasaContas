package br.com.casacontas.shared.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("casacontas.security")
public record SecurityProperties(
    String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl) {}
