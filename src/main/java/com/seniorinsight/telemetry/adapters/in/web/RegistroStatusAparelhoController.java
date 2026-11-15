package com.seniorinsight.telemetry.adapters.in.web;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.application.aparelho.RegistrarStatusAparelho;
import com.seniorinsight.telemetry.application.aparelho.RegistrarStatusAparelho.Comando;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RegistroStatusAparelhoController {
    private final RegistrarStatusAparelho registro;

    RegistroStatusAparelhoController(RegistrarStatusAparelho registro) {
        this.registro = registro;
    }

    @PostMapping("/v1/aparelhos/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void registrar(@Valid @RequestBody Dados dados, @AuthenticationPrincipal Jwt jwt) {
        registro.registrar(jwt.getSubject(), new Comando(dados.idosoId(), dados.sincronizadoEm(),
                dados.nivelBateria(), dados.versaoApp(), dados.modeloCelular(), dados.sistemaOperacional()));
    }

    record Dados(@NotNull UUID idosoId, @NotNull @PastOrPresent Instant sincronizadoEm,
                 @Min(0) @Max(100) Integer nivelBateria, @Size(max = 50) String versaoApp,
                 @Size(max = 100) String modeloCelular, @Size(max = 100) String sistemaOperacional) {
    }
}
