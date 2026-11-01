package com.seniorinsight.telemetry.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;

public interface DeviceSyncStatusRepository extends JpaRepository<DeviceSyncStatus, UUID> {}
