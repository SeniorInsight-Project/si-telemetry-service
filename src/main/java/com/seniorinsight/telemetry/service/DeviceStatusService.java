package com.seniorinsight.telemetry.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;
import com.seniorinsight.telemetry.repository.DeviceSyncStatusRepository;

@Service
public class DeviceStatusService {
    private final DeviceSyncStatusRepository repository;

    public DeviceStatusService(DeviceSyncStatusRepository repository) { this.repository = repository; }

    @Transactional
    public void registerSync(UUID idosoId, Instant syncedAt, Integer battery, String appVersion) {
        DeviceSyncStatus status = repository.findById(idosoId)
                .orElseGet(() -> new DeviceSyncStatus(idosoId, syncedAt, battery, appVersion));
        status.registerSync(syncedAt, battery, appVersion);
        repository.save(status);
    }

    @Transactional
    public void updateConsent(UUID idosoId, List<String> activeDataTypes) {
        DeviceSyncStatus status = repository.findById(idosoId)
                .orElseGet(() -> new DeviceSyncStatus(idosoId, Instant.EPOCH, null, null));
        status.updateActiveDataTypes(activeDataTypes);
        repository.save(status);
    }

    @Transactional(readOnly = true)
    public DeviceStatusResponseDTO getStatus(UUID idosoId) {
        return repository.findById(idosoId)
                .map(status -> new DeviceStatusResponseDTO(status.getIdosoId(), status.getLastSyncAt(),
                        status.getBatteryPercentage(), status.getAppVersion(), status.getActiveDataTypes()))
                .orElse(null);
    }
}
