package com.seniorinsight.telemetry.adapters.in.web;

import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.entrada.ConsultarEstadoAtual;
import com.seniorinsight.telemetry.application.porta.entrada.EstadoAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estado de hoje do idoso. O gateway entrega {@code /api/v1/telemetry/idosos/{id}/hoje} como
 * {@code /v1/idosos/{id}/hoje}.
 */
@RestController
@Tag(name = "Estado atual")
class EstadoAtualController {

    private final ConsultarEstadoAtual consulta;

    EstadoAtualController(ConsultarEstadoAtual consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/v1/idosos/{idosoId}/hoje")
    @Operation(summary = "Passos do dia, último batimento e hora da última atualização")
    EstadoAtual consultar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        return consulta.consultar(idosoId, jwt.getSubject());
    }
}
