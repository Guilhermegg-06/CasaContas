package br.com.casacontas;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DeploymentConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer());

  @Test
  void defaultsToPort8080AndSmallConnectionPool() {
    runner.run(
        context -> {
          var environment = context.getEnvironment();
          assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8080);
          var pool =
              Binder.get(environment).bind("spring.datasource.hikari", HikariConfig.class).get();
          assertThat(pool.getMaximumPoolSize()).isEqualTo(5);
          assertThat(pool.getMinimumIdle()).isEqualTo(1);
        });
  }

  @Test
  void renderUsesSuppliedPortAndVerifiesDatabaseCertificate() {
    runner
        .withPropertyValues(
            "spring.profiles.active=render",
            "PORT=10000",
            "DATABASE_URL=jdbc:postgresql://database.example.test:5432/casacontas",
            "POSTGRES_USER=synthetic-user",
            "POSTGRES_PASSWORD=synthetic-password",
            "JWT_SECRET=synthetic-secret-with-at-least-32-bytes",
            "CORS_ALLOWED_ORIGINS=https://frontend.example.test")
        .run(
            context -> {
              var environment = context.getEnvironment();
              assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(10000);
              assertThat(environment.getProperty("spring.datasource.url"))
                  .isEqualTo("jdbc:postgresql://database.example.test:5432/casacontas");
              var pool =
                  Binder.get(environment)
                      .bind("spring.datasource.hikari", HikariConfig.class)
                      .get();
              assertThat(pool.getMaximumPoolSize()).isEqualTo(5);
              assertThat(pool.getDataSourceProperties())
                  .containsEntry("sslmode", "verify-full")
                  .containsEntry("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
              assertThat(environment.getProperty("casacontas.cors.allowed-origins"))
                  .isEqualTo("https://frontend.example.test");
              assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class))
                  .isFalse();
            });
  }
}
