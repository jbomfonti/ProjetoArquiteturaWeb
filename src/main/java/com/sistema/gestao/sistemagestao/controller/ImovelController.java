package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ImovelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imoveis")
@Validated
public class ImovelController {

    private final ImovelService imovelService;

    public ImovelController(ImovelService imovelService) {
        this.imovelService = imovelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImovelResponse criar(@RequestBody @Valid CriarImovelRequest request) {
        return imovelService.criar(request);
    }

    @GetMapping
    public List<ImovelResponse> listar(@RequestParam(required = false) Boolean disponivel) {
        return imovelService.listar(disponivel);
    }

    @GetMapping("/{id}")
    public ImovelResponse buscar(@PathVariable Long id) {
        return imovelService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ImovelResponse atualizar(@PathVariable Long id,
                                    @RequestBody @Valid AtualizarImovelRequest request) {
        return imovelService.atualizar(id, request);
    }

    @PatchMapping("/{id}/disponibilidade")
    public ImovelResponse alterarDisponibilidade(@PathVariable Long id,
                                                 @RequestBody @Valid AlterarDisponibilidadeRequest request) {
        return imovelService.alterarDisponibilidade(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        imovelService.deletar(id);
    }
}