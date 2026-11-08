package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.aparelho.StatusAparelho;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;

public interface StatusAparelhoRepositorio {

    Optional<StatusAparelho> buscar(UUID idosoId);

    List<TipoDado> buscarTiposAtivosRede(UUID idosoId);

    void registrar(UUID idosoId, Instant sincronizadoEm, Integer nivelBateria, String versaoApp,
                   String modeloCelular, String sistemaOperacional);

    void atualizarConsentimentoRede(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm);
}
