package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.Imovel;
import com.sistema.gestao.sistemagestao.repository.ImovelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImovelService {

    private final ImovelRepository imovelRepository;

    public ImovelService(ImovelRepository imovelRepository) {
        this.imovelRepository = imovelRepository;
    }

    @Transactional
    public ImovelResponse criar(CriarImovelRequest request) {
        Imovel imovel = new Imovel();
        imovel.setNome(request.nome());
        imovel.setDescricao(request.descricao());
        imovel.setEndereco(request.endereco());
        imovel.setPrecoPorNoite(request.precoPorNoite());
        imovel.setCapacidade(request.capacidade());
        return toResponse(imovelRepository.save(imovel));
    }

    @Transactional(readOnly = true)
    public List<ImovelResponse> listar(Boolean disponivel) {
        List<Imovel> imoveis = disponivel != null
                ? imovelRepository.findByDisponivel(disponivel)
                : imovelRepository.findAll();
        return imoveis.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ImovelResponse buscarPorId(Long id) {
        return imovelRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));
    }

    @Transactional
    public ImovelResponse atualizar(Long id, AtualizarImovelRequest request) {
        Imovel imovel = imovelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));
        imovel.setNome(request.nome());
        imovel.setDescricao(request.descricao());
        imovel.setEndereco(request.endereco());
        imovel.setPrecoPorNoite(request.precoPorNoite());
        imovel.setCapacidade(request.capacidade());
        return toResponse(imovelRepository.save(imovel));
    }

    @Transactional
    public ImovelResponse alterarDisponibilidade(Long id, AlterarDisponibilidadeRequest request) {
        Imovel imovel = imovelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));
        imovel.setDisponivel(request.disponivel());
        return toResponse(imovelRepository.save(imovel));
    }

    @Transactional
    public void deletar(Long id) {
        if (!imovelRepository.existsById(id))
            throw new EntityNotFoundException("Imóvel não encontrado");
        imovelRepository.deleteById(id);
    }

    private ImovelResponse toResponse(Imovel i) {
        return new ImovelResponse(
                i.getId(),
                i.getNome(),
                i.getDescricao(),
                i.getEndereco(),
                i.getPrecoPorNoite(),
                i.getCapacidade(),
                i.getDisponivel(),
                i.getCriadoEm()
        );
    }
}