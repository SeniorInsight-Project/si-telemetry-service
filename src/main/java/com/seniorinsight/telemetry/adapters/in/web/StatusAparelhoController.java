package com.seniorinsight.telemetry.adapters.in.web;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.application.aparelho.StatusAparelhoService;
import com.seniorinsight.telemetry.domain.aparelho.StatusAparelho;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StatusAparelhoController {

    private final StatusAparelhoService status;

    StatusAparelhoController(StatusAparelhoService status) {
        this.status = status;
    }

    @GetMapping("/v1/idosos/{idosoId}/status")
    ResponseEntity<StatusAparelho> consultar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.of(status.consultar(idosoId, jwt.getSubject()));
    }

    @PostMapping("/v1/aparelhos/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void registrar(@Valid @RequestBody RegistroStatus request, @AuthenticationPrincipal Jwt jwt) {
        status.registrar(request.idosoId(), jwt.getSubject(), request.sincronizadoEm(), request.nivelBateria(),
                request.versaoApp(), request.modeloCelular(), request.sistemaOperacional());
    }

    record RegistroStatus(
            @NotNull UUID idosoId,
            @NotNull @PastOrPresent Instant sincronizadoEm,
            @Min(0) @Max(100) Integer nivelBateria,
            @Size(max = 50) String versaoApp,
            @Size(max = 100) String modeloCelular,
            @Size(max = 100) String sistemaOperacional) {
    }
}
