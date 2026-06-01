package com.sistema.gestao.sistemagestao.dto;

import com.sistema.gestao.sistemagestao.model.StatusReserva;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusReservaRequest(
        @NotNull StatusReserva status
) {
}