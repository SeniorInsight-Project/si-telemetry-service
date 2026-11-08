package com.seniorinsight.telemetry.adapters.in.web;

import java.util.UUID;

import com.seniorinsight.telemetry.application.medicao.ConsultarEstadoAtual;
import com.seniorinsight.telemetry.application.medicao.ConsultarEstadoAtual.EstadoAtual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EstadoAtualController {

    private final ConsultarEstadoAtual consulta;

    EstadoAtualController(ConsultarEstadoAtual consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/v1/idosos/{idosoId}/hoje")
    EstadoAtual consultar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        return consulta.consultar(idosoId, jwt.getSubject());
    }
}
