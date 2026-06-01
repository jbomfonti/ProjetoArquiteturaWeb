package com.sistema.gestao.sistemagestao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long hospedeId,
        String nomeHospede,
        Long imovelId,
        String nomeImovel,
        LocalDate dataCheckIn,
        LocalDate dataCheckOut,
        Integer numeroHospedes,
        BigDecimal valorTotal,
        String status,
        LocalDateTime criadoEm
) {}
