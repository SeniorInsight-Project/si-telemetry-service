package com.seniorinsight.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;
import com.seniorinsight.telemetry.repository.DeviceSyncStatusRepository;

@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceTest {
    @Mock DeviceSyncStatusRepository repository;
    @InjectMocks DeviceStatusService service;

    @Test
    void registraUltimaSincronizacaoBateriaEVersaoDoAplicativo() {
        UUID idosoId = UUID.randomUUID();
        Instant syncedAt = Instant.parse("2026-11-01T13:15:00Z");
        when(repository.findById(idosoId)).thenReturn(Optional.empty());

        service.registerSync(idosoId, syncedAt, 62, "1.4.0");

        verify(repository).save(argThat(status -> status.getIdosoId().equals(idosoId)
                && status.getLastSyncAt().equals(syncedAt)
                && status.getBatteryPercentage() == 62
                && status.getAppVersion().equals("1.4.0")));
    }

    @Test
    void consentimentoRevogadoAtualizaTiposAtivosNaProjecao() {
        UUID idosoId = UUID.randomUUID();
        DeviceSyncStatus status = new DeviceSyncStatus(idosoId, Instant.now(), 62, "1.4.0");
        status.updateActiveDataTypes(List.of("heart_rate", "steps", "spo2"));
        when(repository.findById(idosoId)).thenReturn(Optional.of(status));

        service.updateConsent(idosoId, List.of("heart_rate", "steps"));

        verify(repository).save(status);
        assertThat(status.getActiveDataTypes()).containsExactly("heart_rate", "steps");
        assertThat(status.getActiveDataTypes()).doesNotContain("spo2");
    }
}
