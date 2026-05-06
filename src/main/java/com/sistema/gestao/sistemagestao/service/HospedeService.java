package com.sistema.gestao.sistemagestao.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.gestao.sistemagestao.model.Hospede;
import com.sistema.gestao.sistemagestao.repository.HospedeRepository;

@Service
public class HospedeService {

    @Autowired
    private HospedeRepository repository;

    public Hospede criar(Hospede hospede) {

        // valida email duplicado
        repository.findByEmail(hospede.getEmail())
                .ifPresent(h -> {
                    throw new RuntimeException("Email já cadastrado");
                });

        // valida cpf duplicado
        repository.findByCpf(hospede.getCpf())
                .ifPresent(h -> {
                    throw new RuntimeException("CPF já cadastrado");
                });

        return repository.save(hospede);
    }

    public List<Hospede> listar() {
        return repository.findAll();
    }

    public Hospede buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hóspede não encontrado"));
    }

    public Hospede atualizar(Long id, Hospede dados) {
        Hospede hospede = buscarPorId(id);

        hospede.setNome(dados.getNome());
        hospede.setEmail(dados.getEmail());
        hospede.setTelefone(dados.getTelefone());
        hospede.setDataNascimento(dados.getDataNascimento());

        return repository.save(hospede);
    }

    public void deletar(Long id) {
        Hospede hospede = buscarPorId(id);
        repository.delete(hospede);
    }
}
