package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Validated
@Tag(name = "Reservas", description = "Gerenciamento de reservas hoteleiras")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar reserva", description = "Cria uma nova reserva e publica o evento no Kafka")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    public ReservaResponse criar(@RequestBody @Valid CriarReservaRequest request) {
        return reservaService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Listar reservas", description = "Lista reservas com filtro opcional por hóspede")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public List<ReservaResponse> listar(@RequestParam(required = false) Long hospedeId) {
        return reservaService.listar(hospedeId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar reserva por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    public ReservaResponse buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    public ReservaResponse atualizarStatus(@PathVariable Long id,
                                           @RequestBody @Valid AtualizarStatusReservaRequest request) {
        return reservaService.atualizarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar/remover reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    public void deletar(@PathVariable Long id) {
        reservaService.deletar(id);
    }
}
