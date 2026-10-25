package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
public record TelemetriaWearableRequestDTO(@JsonProperty("idoso_id") UUID idosoId,@JsonProperty("device_id") String deviceId,@JsonProperty("device_model") String deviceModel,String provider,Instant timestamp,@Valid @JsonProperty("heart_rate") HeartRateDataDTO heartRate,@Valid StepsDataDTO steps,@Valid @JsonProperty("fall_detection") FallDetectionDataDTO fallDetection,@JsonProperty("battery_level") @Min(0) @Max(100) Integer batteryLevel,@JsonProperty("spo2_percentage") @Min(50) @Max(100) Double spo2Percentage,Map<String,Object> metadata) {}
