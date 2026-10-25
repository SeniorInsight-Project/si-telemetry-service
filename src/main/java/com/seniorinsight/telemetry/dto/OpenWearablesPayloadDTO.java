package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
public record OpenWearablesPayloadDTO(@JsonProperty("user_id") String userId,@JsonProperty("device_id") String deviceId,@JsonProperty("device_model") String deviceModel,String provider,@JsonProperty("data_type") String dataType,Instant timestamp,@JsonProperty("heart_rate") HeartRateDataDTO heartRate,StepsDataDTO steps,@JsonProperty("fall_detection") FallDetectionDataDTO fallDetection,@JsonProperty("battery_level") Integer batteryLevel,@JsonProperty("spo2_percentage") Double spo2Percentage,Map<String,Object> metadata) {}
