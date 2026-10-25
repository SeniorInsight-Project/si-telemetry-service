package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record PassosRequestDTO(@JsonProperty("idoso_id") UUID idosoId,@NotNull @Min(0) Integer count,@JsonProperty("distance_meters") @Min(0) Double distanceMeters,@JsonProperty("calories_kcal") @Min(0) Double caloriesKcal,Instant timestamp,@JsonProperty("device_id") String deviceId,@JsonProperty("device_model") String deviceModel) {}
