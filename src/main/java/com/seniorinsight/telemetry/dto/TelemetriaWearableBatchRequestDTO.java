package com.seniorinsight.telemetry.dto;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
public record TelemetriaWearableBatchRequestDTO(
	@NotEmpty @Valid List<TelemetriaWearableRequestDTO> readings,
	@JsonProperty("battery_percentage") @Min(0) @Max(100) Integer batteryPercentage,
	@JsonProperty("app_version") String appVersion,
		@JsonProperty("synced_at") Instant syncedAt) {
	public TelemetriaWearableBatchRequestDTO(List<TelemetriaWearableRequestDTO> readings) {
		this(readings, null, null, null);
	}
}
