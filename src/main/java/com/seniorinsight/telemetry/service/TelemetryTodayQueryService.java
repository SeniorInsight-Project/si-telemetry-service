package com.seniorinsight.telemetry.service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.seniorinsight.telemetry.access.CareAccessClient;
import com.seniorinsight.telemetry.cache.TelemetryTodayCache;
import com.seniorinsight.telemetry.domain.TelemetriaWearable;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;
import com.seniorinsight.telemetry.dto.TelemetryTodayResponseDTO;
import com.seniorinsight.telemetry.repository.TelemetriaWearableRepository;

@Service
public class TelemetryTodayQueryService {
    private final TelemetriaWearableRepository telemetryRepository;
    private final DeviceStatusService statusService;
    private final CareAccessClient careAccessClient;
    private final TelemetryTodayCache cache;
    private final Clock clock;
    private final ZoneId zoneId;
    private final Duration staleAfter;

    public TelemetryTodayQueryService(TelemetriaWearableRepository telemetryRepository,
            DeviceStatusService statusService, CareAccessClient careAccessClient,
            TelemetryTodayCache cache,
            @Value("${seniorinsight.telemetry-today.time-zone:America/Sao_Paulo}") String timeZone,
            @Value("${seniorinsight.telemetry-today.stale-after:48h}") Duration staleAfter) {
        this(telemetryRepository, statusService, careAccessClient, cache,
                Clock.systemUTC(), ZoneId.of(timeZone), staleAfter);
    }

    TelemetryTodayQueryService(TelemetriaWearableRepository telemetryRepository,
            DeviceStatusService statusService, CareAccessClient careAccessClient,
            TelemetryTodayCache cache, Clock clock, ZoneId zoneId, Duration staleAfter) {
        this.telemetryRepository = telemetryRepository;
        this.statusService = statusService;
        this.careAccessClient = careAccessClient;
        this.cache = cache;
        this.clock = clock;
        this.zoneId = zoneId;
        this.staleAfter = staleAfter;
    }

    @Transactional(readOnly = true)
    public TelemetryTodayResponseDTO getToday(UUID idosoId, String userId, String role, String bearerToken) {
        if (userId == null || role == null
                || !careAccessClient.canReadTelemetry(idosoId, userId, role, bearerToken)) {
            throw new TelemetryAccessDeniedException();
        }

        DeviceStatusResponseDTO deviceStatus = statusService.getStatus(idosoId);
        Set<String> authorized = deviceStatus == null || deviceStatus.activeDataTypes() == null
                ? Set.of() : deviceStatus.activeDataTypes().stream()
                        .map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
        String key = "telemetry:today:" + idosoId + ":" + authorized.stream().sorted().collect(Collectors.joining(","));
        Optional<TelemetryTodayResponseDTO> cached = cache.get(key);
                if (cached.isPresent()) {
                        TelemetryTodayResponseDTO response = cached.get();
                        if (isStale(response.lastUpdatedAt(), clock.instant())) {
                                return staleResponse(idosoId, response.lastUpdatedAt());
                        }
                        return response;
                }

        Instant now = clock.instant();
        LocalDate today = now.atZone(zoneId).toLocalDate();
        Instant start = today.atStartOfDay(zoneId).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        List<TelemetriaWearable> readings = telemetryRepository
                .findByIdosoIdAndColetadoEmGreaterThanEqualAndColetadoEmLessThan(idosoId, start, end);
        Instant lastMeasurement = readings.stream().map(TelemetriaWearable::getColetadoEm)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        Instant lastUpdated = max(lastMeasurement, deviceStatus == null ? null : deviceStatus.lastSyncAt());
        boolean stale = isStale(lastUpdated, now);

        TelemetryTodayResponseDTO response;
        if (stale) {
            response = staleResponse(idosoId, lastUpdated);
        } else {
            Integer steps = authorized.contains("steps") ? readings.stream()
                    .map(TelemetriaWearable::getPassos).filter(Objects::nonNull)
                    .reduce(0, Integer::sum) : null;
            Integer heartRate = authorized.contains("heart_rate") ? readings.stream()
                    .filter(r -> r.getFrequenciaCardiaca() != null)
                    .max(Comparator.comparing(TelemetriaWearable::getColetadoEm))
                    .map(TelemetriaWearable::getFrequenciaCardiaca).orElse(null) : null;
            response = new TelemetryTodayResponseDTO(idosoId, steps, heartRate, lastUpdated,
                    TelemetryTodayResponseDTO.UPDATED, null);
        }
        cache.put(key, response);
        return response;
    }

    private static Instant max(Instant first, Instant second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }

        private boolean isStale(Instant lastUpdated, Instant now) {
                return lastUpdated == null || lastUpdated.plus(staleAfter).isBefore(now);
        }

        private static TelemetryTodayResponseDTO staleResponse(UUID idosoId, Instant lastUpdated) {
                return new TelemetryTodayResponseDTO(idosoId, null, null, lastUpdated,
                                TelemetryTodayResponseDTO.NO_RECENT_DATA, "Sem dados recentes");
        }
}