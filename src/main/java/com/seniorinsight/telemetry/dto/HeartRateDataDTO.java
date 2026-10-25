package com.seniorinsight.telemetry.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
public record HeartRateDataDTO(@Min(30) @Max(260) Integer bpm,@JsonProperty("resting_bpm") @Min(30) @Max(200) Integer restingBpm,@JsonProperty("hrv_ms") Double hrvMs) {}
