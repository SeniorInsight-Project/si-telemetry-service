package com.seniorinsight.telemetry.application.medicao;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReferenciaOpenWearables {

    private final byte[] segredo;

    public ReferenciaOpenWearables(@Value("${telemetry.coleta.segredo-vinculo}") String segredo,
                                   @Value("${telemetry.open-wearables.enabled}") boolean ativo) {
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        if ((ativo || this.segredo.length > 0) && this.segredo.length < 32) {
            throw new IllegalArgumentException("A coleta exige um segredo de vínculo estável de pelo menos 32 bytes");
        }
    }

    public Optional<String> gerar(UUID idosoId) {
        if (segredo.length == 0) {
            return Optional.empty();
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return Optional.of("si-telemetry:" + HexFormat.of().formatHex(
                    hmac.doFinal(idosoId.toString().getBytes(StandardCharsets.UTF_8))));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar a referência do provedor", e);
        }
    }
}
