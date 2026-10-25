package com.seniorinsight.telemetry.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
public record StepsDataDTO(@Min(0) Integer count,@JsonProperty("distance_meters") @Min(0) Double distanceMeters,@JsonProperty("calories_kcal") @Min(0) Double caloriesKcal) {}
