package com.seniorinsight.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;
import com.seniorinsight.telemetry.event.TelemetrySignalLostEvent;
import com.seniorinsight.telemetry.event.TelemetrySignalRecoveredEvent;
import com.seniorinsight.telemetry.repository.DeviceSyncStatusRepository;

@ExtendWith(MockitoExtension.class)
class SignalLossDetectionJobTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    @Mock DeviceSyncStatusRepository repository;
    @Mock ApplicationEventPublisher publisher;
    private SignalLossDetectionJob job;

    @BeforeEach
    void setUp() {
        job = new SignalLossDetectionJob(repository, publisher, java.time.Clock.systemUTC(), ZONE,
                Duration.ofHours(3), LocalTime.of(7, 0), LocalTime.of(22, 0));
    }

    @Test
    void publicaUmaPerdaQuandoAJanelaDiurnaSuperaOTresHoras() {
        UUID idosoId = UUID.randomUUID();
        Instant lastSync = local("2026-11-01T09:00:00");
        Instant detectedAt = local("2026-11-01T12:00:00");
        DeviceSyncStatus status = new DeviceSyncStatus(idosoId, lastSync, 62, "1.4.0");
        when(repository.findAll()).thenReturn(List.of(status));

        job.checkAt(detectedAt);
        job.checkAt(detectedAt.plusSeconds(900));

        ArgumentCaptor<TelemetrySignalLostEvent> events = ArgumentCaptor.forClass(TelemetrySignalLostEvent.class);
        verify(publisher, times(1)).publishEvent(events.capture());
        assertThat(events.getValue().type()).isEqualTo("telemetry.signal.lost.v1");
        assertThat(events.getValue().data()).containsEntry("last_sync_at", lastSync);
        assertThat(status.isSignalLost()).isTrue();
    }

    @Test
    void adiaAlertaNoturnoEEmiteAoIniciarAJanelaDiurna() {
        UUID idosoId = UUID.randomUUID();
        DeviceSyncStatus status = new DeviceSyncStatus(idosoId, local("2026-11-01T21:30:00"), null, null);
        when(repository.findAll()).thenReturn(List.of(status));

        job.checkAt(local("2026-11-02T00:30:00"));
        verifyNoInteractions(publisher);

        job.checkAt(local("2026-11-02T07:00:00"));
        verify(publisher).publishEvent(any(TelemetrySignalLostEvent.class));
        assertThat(status.isSignalLost()).isTrue();
    }

    @Test
    void publicaRetornoQuandoOPrimeiroLoteDepoisDaPerdaChega() {
        UUID idosoId = UUID.randomUUID();
        DeviceSyncStatus status = new DeviceSyncStatus(idosoId, local("2026-11-01T09:00:00"), null, null);
        status.markSignalLost();
        when(repository.findById(idosoId)).thenReturn(java.util.Optional.of(status));
        DeviceStatusService statusService = new DeviceStatusService(repository, publisher);
        Instant synchronizedAt = local("2026-11-01T13:15:00");

        statusService.registerSync(idosoId, synchronizedAt, 55, "1.5.0");

        ArgumentCaptor<TelemetrySignalRecoveredEvent> events = ArgumentCaptor.forClass(TelemetrySignalRecoveredEvent.class);
        verify(publisher).publishEvent(events.capture());
        assertThat(events.getValue().type()).isEqualTo("telemetry.signal.recovered.v1");
        assertThat(events.getValue().data()).containsEntry("synchronized_at", synchronizedAt);
        assertThat(status.isSignalLost()).isFalse();
    }

    private Instant local(String value) {
        return java.time.LocalDateTime.parse(value).atZone(ZONE).toInstant();
    }
}
