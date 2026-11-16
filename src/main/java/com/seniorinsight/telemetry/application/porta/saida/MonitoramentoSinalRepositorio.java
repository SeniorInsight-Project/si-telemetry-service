package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;

import com.seniorinsight.telemetry.domain.evento.SinalPerdido;

public interface MonitoramentoSinalRepositorio {

    List<SinalPerdido> registrarPerdas(Instant limiteSincronizacao, Instant detectadoEm);
}
