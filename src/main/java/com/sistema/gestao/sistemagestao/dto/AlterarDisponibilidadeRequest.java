package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarDisponibilidadeRequest(
        @NotNull Boolean disponivel
) {}