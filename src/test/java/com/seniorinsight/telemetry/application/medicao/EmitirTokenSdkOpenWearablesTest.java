package com.seniorinsight.telemetry.application.medicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import org.junit.jupiter.api.Test;

class EmitirTokenSdkOpenWearablesTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    private static final UUID USUARIO = UUID.fromString("0b8f3c52-7d1e-4a3b-9c1d-2e5f6a7b8c9d");

    private final VerificarAcessoIdoso acesso = org.mockito.Mockito.mock(VerificarAcessoIdoso.class);
    private final VincularOpenWearables vincular = org.mockito.Mockito.mock(VincularOpenWearables.class);
    private final OpenWearables openWearables = org.mockito.Mockito.mock(OpenWearables.class);
    private final EmitirTokenSdkOpenWearables emitir = new EmitirTokenSdkOpenWearables(
            acesso, vincular, openWearables, "https://open-wearables.example");

    @Test
    void devolveHostUsuarioEAsDuasCredenciaisDoSdk() {
        when(vincular.obter(IDOSO)).thenReturn(Optional.of(new VinculosOpenWearables.Vinculo(USUARIO, null)));
        when(openWearables.emitirTokenSdk(USUARIO))
                .thenReturn(new OpenWearables.SdkToken("access", "refresh"));

        var resposta = emitir.emitir(IDOSO, "sub-idoso");

        assertThat(resposta).isEqualTo(new EmitirTokenSdkOpenWearables.Resposta(
                "https://open-wearables.example", USUARIO, "access", "refresh"));
        verify(acesso).exigir(IDOSO, "sub-idoso", java.util.Set.of(com.seniorinsight.telemetry.domain.acesso.Papel.IDOSO));
    }

    @Test
    void naoEmiteCredenciaisSemConsentimentoAtivo() {
        when(vincular.obter(IDOSO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emitir.emitir(IDOSO, "sub-idoso"))
                .isInstanceOf(ConsentimentoNaoAtivoException.class);
        verifyNoInteractions(openWearables);
    }
}
