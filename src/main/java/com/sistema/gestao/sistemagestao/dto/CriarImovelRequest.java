package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CriarImovelRequest(
        @NotBlank String nome,
        @NotBlank String descricao,
        @NotBlank String endereco,
        @NotNull @Positive BigDecimal precoPorNoite,
        @NotNull @Positive Integer capacidade
) {}