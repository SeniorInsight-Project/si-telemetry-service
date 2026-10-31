package com.seniorinsight.telemetry.adapters.out.openwearables;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code telemetry.open-wearables.*}: endereço e chave vêm de {@code OPENWEARABLES_BASE_URL} e
 * {@code OPENWEARABLES_API_KEY}. {@code enabled} liga a leitura periódica (si-telemetry-service#5).
 */
@ConfigurationProperties("telemetry.open-wearables")
record PropriedadesOpenWearables(
        boolean enabled,
        String baseUrl,
        String apiKey,
        Duration timeoutConexao,
        Duration timeoutLeitura) {
}
