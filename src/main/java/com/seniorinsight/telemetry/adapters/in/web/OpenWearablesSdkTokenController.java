package com.seniorinsight.telemetry.adapters.in.web;

import java.util.UUID;

import com.seniorinsight.telemetry.application.medicao.EmitirTokenSdkOpenWearables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entrega ao app do próprio idoso as credenciais restritas do SDK do Open Wearables. */
@RestController
@Tag(name = "Open Wearables")
class OpenWearablesSdkTokenController {

    private final EmitirTokenSdkOpenWearables emitir;

    OpenWearablesSdkTokenController(EmitirTokenSdkOpenWearables emitir) {
        this.emitir = emitir;
    }

    @PostMapping("/v1/idosos/{idosoId}/open-wearables/sdk-token")
    @Operation(summary = "Emite as credenciais do SDK do Open Wearables para o próprio idoso")
    Resposta emitir(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        var resposta = emitir.emitir(idosoId, jwt.getSubject());
        return new Resposta(resposta.host(), resposta.userId(), resposta.accessToken(), resposta.refreshToken());
    }

    record Resposta(String host, UUID userId, String accessToken, String refreshToken) {
    }
}
