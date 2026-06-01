package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Validated
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@RequestBody @Valid CriarReservaRequest request) {
        return reservaService.criar(request);
    }

    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) Long hospedeId) {
        return reservaService.listar(hospedeId);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    public ReservaResponse atualizarStatus(@PathVariable Long id,
                                           @RequestBody @Valid AtualizarStatusReservaRequest request) {
        return reservaService.atualizarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        reservaService.deletar(id);
    }
}