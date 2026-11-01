package com.seniorinsight.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.seniorinsight.telemetry.access.CareAccessClient;
import com.seniorinsight.telemetry.cache.TelemetryTodayCache;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;
import com.seniorinsight.telemetry.domain.TelemetriaWearable;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;
import com.seniorinsight.telemetry.dto.TelemetryTodayResponseDTO;
import com.seniorinsight.telemetry.repository.TelemetriaWearableRepository;

@ExtendWith(MockitoExtension.class)
class TelemetryTodayQueryServiceTest {
    private static final UUID IDOSO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-01T15:00:00Z");

    @Mock TelemetriaWearableRepository telemetryRepository;
    @Mock DeviceStatusService statusService;
    @Mock CareAccessClient careAccessClient;
    @Mock TelemetryTodayCache cache;
    private TelemetryTodayQueryService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryTodayQueryService(telemetryRepository, statusService, careAccessClient,
                cache, Clock.fixed(NOW, ZoneOffset.UTC), ZoneOffset.UTC, Duration.ofHours(48));
        when(careAccessClient.canReadTelemetry(any(), anyString(), anyString(), any()))
                .thenReturn(true);
        when(cache.get(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void idosoAutorizadoVePassosEBatimento() {
        givenStatus(List.of("heart_rate", "steps"), NOW.minus(Duration.ofHours(1)));
        whenReadings(reading(1_000, 70, NOW.minus(Duration.ofHours(3))), reading(1_340, 72, NOW.minus(Duration.ofMinutes(10))));

        TelemetryTodayResponseDTO response = service.getToday(IDOSO_ID, "idoso-1", "IDOSO", "token");

        assertThat(response.stepsToday()).isEqualTo(2_340);
        assertThat(response.heartRate()).isEqualTo(72);
        assertThat(response.status()).isEqualTo(TelemetryTodayResponseDTO.UPDATED);
        verify(careAccessClient).canReadTelemetry(IDOSO_ID, "idoso-1", "IDOSO", "token");
    }

    @Test
    void familiarVinculadoVeOMesmoEstado() {
        givenStatus(List.of("heart_rate", "steps"), NOW.minus(Duration.ofMinutes(5)));
        whenReadings(reading(2_340, 72, NOW.minus(Duration.ofMinutes(5))));

        TelemetryTodayResponseDTO response = service.getToday(IDOSO_ID, "familiar-1", "FAMILIAR_APOIO", "token");

        assertThat(response.stepsToday()).isEqualTo(2_340);
        assertThat(response.heartRate()).isEqualTo(72);
        verify(careAccessClient).canReadTelemetry(IDOSO_ID, "familiar-1", "FAMILIAR_APOIO", "token");
    }

    @Test
    void naoExpõeBatimentoRevogado() {
        givenStatus(List.of("steps"), NOW.minus(Duration.ofMinutes(5)));
        whenReadings(reading(2_340, 72, NOW.minus(Duration.ofMinutes(5))));

        TelemetryTodayResponseDTO response = service.getToday(IDOSO_ID, "familiar-1", "FAMILIAR_ADMIN", "token");

        assertThat(response.stepsToday()).isEqualTo(2_340);
        assertThat(response.heartRate()).isNull();
    }

    @Test
    void naoExpõePassosRevogados() {
        givenStatus(List.of("heart_rate"), NOW.minus(Duration.ofMinutes(5)));
        whenReadings(reading(2_340, 72, NOW.minus(Duration.ofMinutes(5))));

        TelemetryTodayResponseDTO response = service.getToday(IDOSO_ID, "familiar-1", "FAMILIAR_ADMIN", "token");

        assertThat(response.stepsToday()).isNull();
        assertThat(response.heartRate()).isEqualTo(72);
    }

    @Test
    void dadosComMaisDeQuarentaEOitoHorasRetornamMensagemSemNumeros() {
        givenStatus(List.of("heart_rate", "steps"), NOW.minus(Duration.ofHours(49)));
        when(telemetryRepository.findByIdosoIdAndColetadoEmGreaterThanEqualAndColetadoEmLessThan(
                eq(IDOSO_ID), any(), any())).thenReturn(List.of());

        TelemetryTodayResponseDTO response = service.getToday(IDOSO_ID, "familiar-1", "FAMILIAR_ADMIN", "token");

        assertThat(response.stepsToday()).isNull();
        assertThat(response.heartRate()).isNull();
        assertThat(response.status()).isEqualTo(TelemetryTodayResponseDTO.NO_RECENT_DATA);
        assertThat(response.mensagem()).isEqualTo("Sem dados recentes");
        assertThat(response.lastUpdatedAt()).isEqualTo(NOW.minus(Duration.ofHours(49)));
    }

    @Test
    void reutilizaCacheSemConsultarBancoNovamente() {
        givenStatus(List.of("heart_rate", "steps"), NOW.minus(Duration.ofMinutes(5)));
        TelemetryTodayResponseDTO cached = new TelemetryTodayResponseDTO(IDOSO_ID, 2_340, 72,
                NOW.minus(Duration.ofMinutes(5)), TelemetryTodayResponseDTO.UPDATED, null);
        when(cache.get(anyString())).thenReturn(Optional.of(cached));

        assertThat(service.getToday(IDOSO_ID, "familiar-1", "FAMILIAR_ADMIN", "token")).isEqualTo(cached);
        verifyNoInteractions(telemetryRepository);
    }

    private void givenStatus(List<String> activeTypes, Instant lastSync) {
        DeviceSyncStatus status = new DeviceSyncStatus(IDOSO_ID, lastSync, 62, "1.4.0");
        status.updateActiveDataTypes(activeTypes);
        when(statusService.getStatus(IDOSO_ID)).thenReturn(new DeviceStatusResponseDTO(
                IDOSO_ID, status.getLastSyncAt(), status.getBatteryPercentage(), status.getAppVersion(),
                status.getActiveDataTypes()));
    }

    private void whenReadings(TelemetriaWearable... readings) {
        when(telemetryRepository.findByIdosoIdAndColetadoEmGreaterThanEqualAndColetadoEmLessThan(
                eq(IDOSO_ID), any(), any())).thenReturn(List.of(readings));
    }

    private TelemetriaWearable reading(Integer steps, Integer heartRate, Instant collectedAt) {
        return TelemetriaWearable.from(IDOSO_ID, "device", "watch", "OPEN_WEARABLES",
                heartRate, null, null, steps, null, null, false, null, null, null, null, null, collectedAt);
    }
}