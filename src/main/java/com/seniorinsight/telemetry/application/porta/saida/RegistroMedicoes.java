package com.seniorinsight.telemetry.application.porta.saida;

import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.Leitura;

public interface RegistroMedicoes {

    void registrar(UUID idosoId, List<Leitura> leituras);
}
