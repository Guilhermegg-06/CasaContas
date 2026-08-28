package br.com.casacontas;

import org.springframework.boot.SpringApplication;

public class TestCasaContasApplication {

  public static void main(String[] args) {
    SpringApplication.from(CasaContasApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
