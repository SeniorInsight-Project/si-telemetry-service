package com.seniorinsight.telemetry.adapters.in.web;

import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.entrada.ConsultarStatusAparelho;
import com.seniorinsight.telemetry.application.porta.entrada.StatusAparelho;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Status do celular do idoso. O gateway entrega {@code /api/v1/telemetry/idosos/{id}/status} como
 * {@code /v1/idosos/{id}/status}.
 */
@RestController
@Tag(name = "Aparelho")
class StatusAparelhoController {

    private final ConsultarStatusAparelho consulta;

    StatusAparelhoController(ConsultarStatusAparelho consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/v1/idosos/{idosoId}/status")
    @Operation(summary = "Última sincronização, bateria, versão do app e tipos de dado ativos")
    StatusAparelho consultar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        return consulta.consultar(idosoId, jwt.getSubject());
    }
}
