package com.seniorinsight.telemetry.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_sync_status")
public class DeviceSyncStatus {
    @Id
    @Column(name = "idoso_id", nullable = false)
    private UUID idosoId;
    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;
    @Column(name = "battery_percentage")
    private Integer batteryPercentage;
    @Column(name = "app_version", length = 50)
    private String appVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "active_data_types", columnDefinition = "jsonb", nullable = false)
    private List<String> activeDataTypes = new ArrayList<>();

    protected DeviceSyncStatus() {}

    public DeviceSyncStatus(UUID idosoId, Instant lastSyncAt, Integer batteryPercentage,
                            String appVersion) {
        this.idosoId = idosoId;
        this.lastSyncAt = lastSyncAt;
        this.batteryPercentage = batteryPercentage;
        this.appVersion = appVersion;
    }

    public void registerSync(Instant syncedAt, Integer battery, String version) {
        this.lastSyncAt = syncedAt;
        if (battery != null) this.batteryPercentage = battery;
        if (version != null && !version.isBlank()) this.appVersion = version;
    }

    public void updateActiveDataTypes(List<String> types) {
        this.activeDataTypes = types == null ? new ArrayList<>() : new ArrayList<>(types);
    }

    public UUID getIdosoId() { return idosoId; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public Integer getBatteryPercentage() { return batteryPercentage; }
    public String getAppVersion() { return appVersion; }
    public List<String> getActiveDataTypes() { return List.copyOf(activeDataTypes); }
}
