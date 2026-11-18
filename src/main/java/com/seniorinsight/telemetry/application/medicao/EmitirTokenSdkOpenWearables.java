package com.seniorinsight.telemetry.application.medicao;

import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables.Vinculo;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Prepara as credenciais restritas que o app usa para conectar o SDK ao Open Wearables.
 *
 * <p>O host devolvido é o endereço público da API do Open Wearables, alcançável pelo celular. Não é o
 * {@code base-url}: esse é o endereço interno (ex.: {@code http://ow-app:8000} na rede do Docker), que o
 * aparelho não resolve. Com ele o SDK conecta ao Health Connect, mas nenhum envio de dados chega.</p>
 */
@Service
public class EmitirTokenSdkOpenWearables {

    private static final Set<Papel> SOMENTE_IDOSO = Set.of(Papel.IDOSO);

    private final VerificarAcessoIdoso acesso;
    private final VincularOpenWearables vincular;
    private final OpenWearables openWearables;
    private final String apiHost;

    public EmitirTokenSdkOpenWearables(VerificarAcessoIdoso acesso, VincularOpenWearables vincular,
                                       OpenWearables openWearables,
                                       @Value("${telemetry.open-wearables.public-url}") String apiHost) {
        this.acesso = acesso;
        this.vincular = vincular;
        this.openWearables = openWearables;
        this.apiHost = apiHost;
    }

    public Resposta emitir(UUID idosoId, String sub) {
        acesso.exigir(idosoId, sub, SOMENTE_IDOSO);
        Vinculo vinculo = vincular.obter(idosoId)
                .orElseThrow(() -> new ConsentimentoNaoAtivoException(idosoId));
        OpenWearables.SdkToken token = openWearables.emitirTokenSdk(vinculo.usuarioId());
        return new Resposta(apiHost, vinculo.usuarioId(), token.accessToken(), token.refreshToken());
    }

    public record Resposta(String host, UUID userId, String accessToken, String refreshToken) {
    }
}
