package com.sistema.gestao.sistemagestao.dto;

import java.time.LocalDate;

public record AtualizarHospedeRequest(
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento
) {}