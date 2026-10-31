package com.seniorinsight.telemetry;

import org.springframework.boot.SpringApplication;

/** Sobe o serviço localmente com PostgreSQL e RabbitMQ do Testcontainers: {@code mvn spring-boot:test-run}. */
public class TestTelemetryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(TelemetryServiceApplication::main).with(ContainersConfig.class).run(args);
    }
}
