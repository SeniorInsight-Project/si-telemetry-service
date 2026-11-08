package com.seniorinsight.telemetry.adapters.out.care;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code telemetry.care-service.*}: o endereço vem de {@code CARE_SERVICE_URL}. */
@ConfigurationProperties("telemetry.care-service")
record PropriedadesCareService(String baseUrl, Duration timeoutConexao, Duration timeoutLeitura) {
}
