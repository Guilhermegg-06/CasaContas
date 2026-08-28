package br.com.casacontas.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DocumentationSecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain documentationSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/swagger-ui/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    return http.build();
  }
}
