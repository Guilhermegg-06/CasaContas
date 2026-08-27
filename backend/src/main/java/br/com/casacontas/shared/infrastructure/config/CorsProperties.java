package br.com.casacontas.shared.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("casacontas.cors")
public record CorsProperties(List<String> allowedOrigins) {}
