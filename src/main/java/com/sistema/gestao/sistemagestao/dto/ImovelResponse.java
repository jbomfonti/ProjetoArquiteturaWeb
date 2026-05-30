package com.sistema.gestao.sistemagestao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImovelResponse(
        Long id,
        String nome,
        String descricao,
        String endereco,
        BigDecimal precoPorNoite,
        Integer capacidade,
        Boolean disponivel,
        LocalDateTime criadoEm
) {}