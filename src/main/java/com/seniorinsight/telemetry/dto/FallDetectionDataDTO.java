package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;
public record FallDetectionDataDTO(@JsonProperty("fall_detected") Boolean fallDetected,@JsonProperty("impact_force_g") Double impactForceG,Double confidence,@JsonProperty("fall_timestamp") Instant fallTimestamp) {}
