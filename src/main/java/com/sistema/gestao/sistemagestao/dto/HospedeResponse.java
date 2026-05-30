package com.sistema.gestao.sistemagestao.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HospedeResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String cpf,
        LocalDate dataNascimento,
        LocalDateTime dataCadastro
) {}