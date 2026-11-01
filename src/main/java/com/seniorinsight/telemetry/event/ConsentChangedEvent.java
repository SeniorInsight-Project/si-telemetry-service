package com.seniorinsight.telemetry.event;

import java.util.List;
import java.util.UUID;

public record ConsentChangedEvent(UUID idosoId, List<String> activeDataTypes) {}
