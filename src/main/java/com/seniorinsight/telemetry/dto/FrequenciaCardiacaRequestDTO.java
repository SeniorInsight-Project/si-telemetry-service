package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record FrequenciaCardiacaRequestDTO(@JsonProperty("idoso_id") UUID idosoId,@NotNull @Min(30) @Max(260) Integer bpm,@JsonProperty("resting_bpm") @Min(30) @Max(200) Integer restingBpm,@JsonProperty("hrv_ms") Double hrvMs,Instant timestamp,@JsonProperty("device_id") String deviceId,@JsonProperty("device_model") String deviceModel) {}
