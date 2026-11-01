package com.seniorinsight.telemetry.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.seniorinsight.telemetry.access.CareAccessClient;

@ExtendWith(MockitoExtension.class)
class DeviceStatusQueryServiceTest {
    @Mock DeviceStatusService statusService;
    @Mock CareAccessClient careAccessClient;
    @InjectMocks DeviceStatusQueryService service;

    @Test
    void rejeitaFamiliarSemVinculoComOIdoso() {
        UUID idosoId = UUID.randomUUID();
        when(careAccessClient.canReadTelemetry(idosoId, "familiar-2", "FAMILIAR_APOIO", "token"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getStatus(idosoId, "familiar-2", "FAMILIAR_APOIO", "token"))
                .isInstanceOf(TelemetryAccessDeniedException.class);
        verifyNoInteractions(statusService);
    }
}
