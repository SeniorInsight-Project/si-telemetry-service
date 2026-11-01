package com.seniorinsight.telemetry.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.seniorinsight.telemetry.access.CareAccessClient;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;

@Service
public class DeviceStatusQueryService {
    private final DeviceStatusService statusService;
    private final CareAccessClient careAccessClient;

    public DeviceStatusQueryService(DeviceStatusService statusService, CareAccessClient careAccessClient) {
        this.statusService = statusService;
        this.careAccessClient = careAccessClient;
    }

    public DeviceStatusResponseDTO getStatus(UUID idosoId, String userId, String role, String bearerToken) {
        if (userId == null || role == null || !careAccessClient.canReadTelemetry(idosoId, userId, role, bearerToken)) {
            throw new TelemetryAccessDeniedException();
        }
        return statusService.getStatus(idosoId);
    }
}
