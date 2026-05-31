package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CriarReservaRequest(
        @NotNull Long hospedeId,
        @NotNull Long imovelId,
        @NotNull @FutureOrPresent LocalDate dataCheckIn,
        @NotNull LocalDate dataCheckOut,
        @NotNull @Min(1) Integer numeroHospedes
) {}