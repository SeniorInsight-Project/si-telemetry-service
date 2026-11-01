package com.seniorinsight.telemetry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;
import com.seniorinsight.telemetry.service.DeviceStatusQueryService;
import com.seniorinsight.telemetry.service.WearableIntegrationService;

class TelemetriaWearableControllerTest {
    private final WearableIntegrationService wearableService = mock(WearableIntegrationService.class);
    private final DeviceStatusQueryService statusService = mock(DeviceStatusQueryService.class);
    private final TelemetriaWearableController controller = new TelemetriaWearableController(wearableService, statusService);

    @Test
    void retornaStatusDoCelularParaFamiliarVinculado() {
        UUID idosoId = UUID.randomUUID();
        var expected = new DeviceStatusResponseDTO(idosoId, Instant.parse("2026-11-01T13:15:00Z"), 62,
                "1.4.0", List.of("heart_rate", "steps"));
        when(statusService.getStatus(idosoId, "familiar-1", "FAMILIAR_ADMIN", "token"))
                .thenReturn(expected);

        ResponseEntity<DeviceStatusResponseDTO> response = controller.status(idosoId, "familiar-1",
                "FAMILIAR_ADMIN", "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void exigeIdentidadePropagadaPeloGateway() {
        ResponseEntity<DeviceStatusResponseDTO> response = controller.status(UUID.randomUUID(), null,
                "FAMILIAR_ADMIN", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(statusService);
    }
}
