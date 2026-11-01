package com.seniorinsight.telemetry.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.seniorinsight.telemetry.service.TelemetryAccessDeniedException;

@RestControllerAdvice
public class TelemetryExceptionHandler {
    @ExceptionHandler(TelemetryAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void accessDenied() {}
}
