package com.seniorinsight.telemetry.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.seniorinsight.telemetry.domain.DeviceSyncStatus;
import com.seniorinsight.telemetry.event.TelemetrySignalLostEvent;
import com.seniorinsight.telemetry.repository.DeviceSyncStatusRepository;

@Component
public class SignalLossDetectionJob {
    private final DeviceSyncStatusRepository repository;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;
    private final Duration threshold;
    private final LocalTime dayStart;
    private final LocalTime dayEnd;
    private final ZoneId zoneId;

    public SignalLossDetectionJob(DeviceSyncStatusRepository repository, ApplicationEventPublisher publisher,
            @Value("${seniorinsight.sync-monitoring.time-zone:America/Sao_Paulo}") ZoneId zoneId,
            @Value("${seniorinsight.sync-monitoring.threshold:PT3H}") Duration threshold,
            @Value("${seniorinsight.sync-monitoring.day-start:07:00}") LocalTime dayStart,
            @Value("${seniorinsight.sync-monitoring.day-end:22:00}") LocalTime dayEnd) {
        this(repository, publisher, Clock.systemUTC(), zoneId, threshold, dayStart, dayEnd);
    }

    public SignalLossDetectionJob(DeviceSyncStatusRepository repository, ApplicationEventPublisher publisher,
            Clock clock, ZoneId zoneId, Duration threshold, LocalTime dayStart, LocalTime dayEnd) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.zoneId = zoneId;
        this.threshold = threshold;
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
    }

    @Scheduled(fixedDelayString = "${seniorinsight.sync-monitoring.interval-ms:900000}")
    @Transactional
    public void checkForMissingSynchronizations() {
        checkAt(Instant.now(clock));
    }

    @Transactional
    public void checkAt(Instant now) {
        ZonedDateTime localNow = now.atZone(zoneId);
        if (!isDaytime(localNow.toLocalTime())) return;
        for (DeviceSyncStatus status : repository.findAll()) {
            if (isLate(status, now) && status.markSignalLost()) {
                repository.save(status);
                publisher.publishEvent(TelemetrySignalLostEvent.create(status.getIdosoId(),
                        status.getLastSyncAt(), now));
            }
        }
    }

    private boolean isLate(DeviceSyncStatus status, Instant now) {
        return !status.getLastSyncAt().plus(threshold).isAfter(now);
    }

    private boolean isDaytime(LocalTime time) {
        return !time.isBefore(dayStart) && !time.isAfter(dayEnd);
    }
}