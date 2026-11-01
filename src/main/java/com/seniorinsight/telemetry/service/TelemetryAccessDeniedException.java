package com.seniorinsight.telemetry.service;

public class TelemetryAccessDeniedException extends RuntimeException {
    public TelemetryAccessDeniedException() { super("Usuário não possui vínculo de leitura com o idoso."); }
}
