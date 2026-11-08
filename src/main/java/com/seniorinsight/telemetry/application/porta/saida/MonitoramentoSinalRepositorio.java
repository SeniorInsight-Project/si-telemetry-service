package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.evento.SinalPerdido;

public interface MonitoramentoSinalRepositorio {

    List<SinalPerdido> registrarPerdas(Instant limiteSincronizacao, Instant detectadoEm);

    boolean registrarRetorno(UUID idosoId, Instant sincronizadoEm);
}
