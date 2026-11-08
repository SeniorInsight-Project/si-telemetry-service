package com.seniorinsight.telemetry.application.medicao;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class ColetarMedicoesAutorizadasTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final Instant INICIO = Instant.parse("2026-11-01T17:00:00Z");
    private static final Instant FIM = Instant.parse("2026-11-01T17:15:00Z");

    private final StatusAparelhoRepositorio consentimentos = mock(StatusAparelhoRepositorio.class);
    private final OpenWearables openWearables = mock(OpenWearables.class);
    private final RegistroMedicoes registro = mock(RegistroMedicoes.class);
    private final VinculosOpenWearables vinculos = mock(VinculosOpenWearables.class);
    private final ColetarMedicoesAutorizadas coleta = new ColetarMedicoesAutorizadas(
            consentimentos, openWearables, registro, TransactionOperations.withoutTransaction(), vinculos);

    @Test
    void todosRevogadosPausamAConsultaAoProvedor() {
        when(consentimentos.buscarTiposAtivosRede(IDOSO)).thenReturn(List.of());

        coleta.coletar(IDOSO, USUARIO, INICIO, FIM);

        verifyNoInteractions(openWearables, registro);
    }

    @Test
    void revogacaoDuranteAConsultaImpedeGravarPassos() {
        when(consentimentos.buscarTiposAtivosRede(IDOSO))
                .thenReturn(List.of(TipoDado.PASSOS, TipoDado.BATIMENTOS))
                .thenReturn(List.of(TipoDado.BATIMENTOS));
        Set<String> autorizados = Set.of("steps", "heart_rate", "resting_heart_rate");
        when(vinculos.inicioAutorizado(IDOSO)).thenReturn(Optional.of(INICIO));
        var passos = new Leitura("steps", INICIO, 100, "count", "teste");
        var batimento = new Leitura("heart_rate", INICIO, 72, "bpm", "teste");
        var inesperada = new Leitura("oxygen_saturation", INICIO, 98, "percent", "teste");
        when(openWearables.buscarLeituras(USUARIO, INICIO, FIM, autorizados))
                .thenReturn(List.of(passos, batimento, inesperada));

        coleta.coletar(IDOSO, USUARIO, INICIO, FIM);

        var ordem = inOrder(consentimentos, openWearables, registro);
        ordem.verify(consentimentos).buscarTiposAtivosRede(IDOSO);
        ordem.verify(openWearables).buscarLeituras(USUARIO, INICIO, FIM, autorizados);
        ordem.verify(consentimentos).bloquearConsentimentos(IDOSO);
        ordem.verify(consentimentos).buscarTiposAtivosRede(IDOSO);
        ordem.verify(registro).registrar(IDOSO, List.of(batimento));
    }
}
