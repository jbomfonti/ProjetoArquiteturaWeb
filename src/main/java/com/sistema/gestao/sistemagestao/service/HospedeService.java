package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.model.Hospede;
import com.sistema.gestao.sistemagestao.repository.HospedeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HospedeService {

    private final HospedeRepository hospedeRepository;

    public HospedeService(HospedeRepository hospedeRepository) {
        this.hospedeRepository = hospedeRepository;
    }

    @Transactional
    public HospedeResponse criar(CriarHospedeRequest request) {
        if (hospedeRepository.existsByEmail(request.email()))
            throw new IllegalStateException("E-mail já cadastrado");
        if (hospedeRepository.existsByCpf(request.cpf()))
            throw new IllegalStateException("CPF já cadastrado");

        Hospede hospede = new Hospede();
        hospede.setNome(request.nome());
        hospede.setEmail(request.email());
        hospede.setTelefone(request.telefone());
        hospede.setCpf(request.cpf());
        hospede.setDataNascimento(request.dataNascimento());

        return toResponse(hospedeRepository.save(hospede));
    }

    @Transactional(readOnly = true)
    public List<HospedeResponse> listar() {
        return hospedeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HospedeResponse buscarPorId(Long id) {
        return hospedeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));
    }

    @Transactional
    public HospedeResponse atualizar(Long id, AtualizarHospedeRequest request) {
        Hospede hospede = hospedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));

        if (!hospede.getEmail().equals(request.email()) &&
                hospedeRepository.existsByEmail(request.email()))
            throw new IllegalStateException("E-mail já cadastrado por outro hóspede");

        hospede.setNome(request.nome());
        hospede.setEmail(request.email());
        hospede.setTelefone(request.telefone());
        hospede.setDataNascimento(request.dataNascimento());

        return toResponse(hospedeRepository.save(hospede));
    }

    @Transactional
    public void deletar(Long id) {
        if (!hospedeRepository.existsById(id))
            throw new EntityNotFoundException("Hóspede não encontrado");
        hospedeRepository.deleteById(id);
    }

    private HospedeResponse toResponse(Hospede h) {
        return new HospedeResponse(
                h.getId(),
                h.getNome(),
                h.getEmail(),
                h.getTelefone(),
                h.getCpf(),
                h.getDataNascimento(),
                h.getDataCadastro()
        );
    }
}