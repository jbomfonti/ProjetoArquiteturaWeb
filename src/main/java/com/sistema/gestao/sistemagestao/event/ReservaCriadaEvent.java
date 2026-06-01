package com.sistema.gestao.sistemagestao.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaCriadaEvent(
        Long reservaId,
        Long hospedeId,
        Long imovelId,
        LocalDate dataCheckIn,
        LocalDate dataCheckOut,
        BigDecimal valorTotal
) {}
