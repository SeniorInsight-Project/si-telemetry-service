package com.seniorinsight.telemetry.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
public record TelemetriaWearableBatchRequestDTO(@NotEmpty @Valid List<TelemetriaWearableRequestDTO> readings) {}
