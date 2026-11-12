package com.seniorinsight.telemetry.adapters.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seniorinsight.telemetry.application.porta.entrada.IngerirTelemetria;
import com.seniorinsight.telemetry.application.porta.entrada.ResultadoIngestao;
import com.seniorinsight.telemetry.application.porta.entrada.TelemetriaRecebida;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestão de telemetria pelo app do idoso. O gateway entrega {@code /api/v1/telemetry/idosos/{id}/telemetria} como
 * {@code /v1/idosos/{id}/telemetria}. O corpo mantém os nomes em snake_case do payload que o app já enviava ao
 * care-service.
 */
@RestController
@Tag(name = "Ingestão")
class TelemetriaController {

    static final int MAXIMO_LEITURAS_POR_LOTE = 500;

    private final IngerirTelemetria ingestao;

    TelemetriaController(IngerirTelemetria ingestao) {
        this.ingestao = ingestao;
    }

    @PostMapping("/v1/idosos/{idosoId}/telemetria")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envia uma leitura do relógio")
    ResultadoIngestao receber(@PathVariable UUID idosoId, @Valid @RequestBody Leitura leitura,
                              @AuthenticationPrincipal Jwt jwt) {
        return ingestao.ingerir(idosoId, jwt.getSubject(), List.of(leitura.paraEntrada()));
    }

    @PostMapping("/v1/idosos/{idosoId}/telemetria/lote")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envia várias leituras do relógio (até 500)")
    ResultadoIngestao receberLote(@PathVariable UUID idosoId, @Valid @RequestBody Lote lote,
                                  @AuthenticationPrincipal Jwt jwt) {
        return ingestao.ingerir(idosoId, jwt.getSubject(),
                lote.readings().stream().map(Leitura::paraEntrada).toList());
    }

    record Lote(@NotEmpty @Size(max = MAXIMO_LEITURAS_POR_LOTE) List<@Valid @NotNull Leitura> readings) {
    }

    record Leitura(
            @Size(max = 100) String provider,
            @NotNull Instant timestamp,
            @JsonProperty("heart_rate") @Valid FrequenciaCardiaca heartRate,
            @Valid Passos steps,
            @JsonProperty("fall_detection") @Valid Queda fallDetection,
            @JsonProperty("battery_level") Integer batteryLevel,
            @JsonProperty("spo2_percentage") Double spo2Percentage) {

        TelemetriaRecebida paraEntrada() {
            return new TelemetriaRecebida(provider, timestamp,
                    heartRate == null ? null : heartRate.bpm(),
                    heartRate == null ? null : heartRate.restingBpm(),
                    heartRate == null ? null : heartRate.hrvMs(),
                    steps == null ? null : steps.count(),
                    steps == null ? null : steps.distanceMeters(),
                    steps == null ? null : steps.caloriesKcal(),
                    spo2Percentage,
                    fallDetection == null ? null : fallDetection.fallDetected(),
                    fallDetection == null ? null : fallDetection.impactForceG(),
                    fallDetection == null ? null : fallDetection.confidence(),
                    fallDetection == null ? null : fallDetection.fallTimestamp(),
                    batteryLevel);
        }
    }

    record FrequenciaCardiaca(Integer bpm, @JsonProperty("resting_bpm") Integer restingBpm,
                              @JsonProperty("hrv_ms") Double hrvMs) {
    }

    record Passos(Integer count, @JsonProperty("distance_meters") Double distanceMeters,
                  @JsonProperty("calories_kcal") Double caloriesKcal) {
    }

    record Queda(@JsonProperty("fall_detected") Boolean fallDetected,
                 @JsonProperty("impact_force_g") Double impactForceG, Double confidence,
                 @JsonProperty("fall_timestamp") Instant fallTimestamp) {
    }
}
