package com.seniorinsight.telemetry.adapters.in.web;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Respostas de erro no formato Problem Details (RFC 9457). */
@RestControllerAdvice
class TratamentoErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoErros.class);

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegado(AcessoNegadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(VinculoIndisponivelException.class)
    ProblemDetail vinculoIndisponivel(VinculoIndisponivelException e) {
        log.warn("Consulta de vínculo falhou: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível verificar o vínculo com o idoso. Tente de novo em instantes.");
    }
}
