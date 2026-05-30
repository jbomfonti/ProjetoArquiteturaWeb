package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.service.HospedeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospedes")
@Validated
public class HospedeController {

    private final HospedeService hospedeService;

    public HospedeController(HospedeService hospedeService) {
        this.hospedeService = hospedeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospedeResponse criar(@RequestBody @Valid CriarHospedeRequest request) {
        return hospedeService.criar(request);
    }

    @GetMapping
    public List<HospedeResponse> listar() {
        return hospedeService.listar();
    }

    @GetMapping("/{id}")
    public HospedeResponse buscar(@PathVariable Long id) {
        return hospedeService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public HospedeResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid AtualizarHospedeRequest request) {
        return hospedeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        hospedeService.deletar(id);
    }
}