package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
public record DeteccaoQuedaRequestDTO(@JsonProperty("idoso_id") UUID idosoId,@NotNull @JsonProperty("fall_detected") Boolean fallDetected,@JsonProperty("impact_force_g") Double impactForceG,Double confidence,Instant timestamp,Double latitude,Double longitude,@JsonProperty("battery_level") Integer batteryLevel,@JsonProperty("device_id") String deviceId,@JsonProperty("device_model") String deviceModel,String observacao) {}
