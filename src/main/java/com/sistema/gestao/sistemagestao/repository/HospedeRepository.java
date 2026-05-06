package com.sistema.gestao.sistemagestao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sistema.gestao.sistemagestao.model.Hospede;

public interface HospedeRepository extends JpaRepository<Hospede, Long> {

    Optional<Hospede> findByEmail(String email);

    Optional<Hospede> findByCpf(String cpf);
}
