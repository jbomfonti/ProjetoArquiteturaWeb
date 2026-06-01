package com.sistema.gestao.sistemagestao.event;

public record PagamentoResultadoEvent(
        Long reservaId,
        Long pagamentoId,
        String status,  // APROVADO | RECUSADO | ESTORNADO
        String motivo   // preenchido quando RECUSADO ou ESTORNADO
) {}
