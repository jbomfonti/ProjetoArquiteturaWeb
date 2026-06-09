package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.service.HospedeService;
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
@RequestMapping("/api/hospedes")
@Validated
@Tag(name = "Hóspedes", description = "Gerenciamento de hóspedes")
public class HospedeController {

    private final HospedeService hospedeService;

    public HospedeController(HospedeService hospedeService) {
        this.hospedeService = hospedeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar hóspede", description = "Cria um novo hóspede no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hóspede criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    })
    public HospedeResponse criar(@RequestBody @Valid CriarHospedeRequest request) {
        return hospedeService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os hóspedes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public List<HospedeResponse> listar() {
        return hospedeService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar hóspede por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hóspede encontrado"),
            @ApiResponse(responseCode = "404", description = "Hóspede não encontrado")
    })
    public HospedeResponse buscar(@PathVariable Long id) {
        return hospedeService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do hóspede")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hóspede atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Hóspede não encontrado")
    })
    public HospedeResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid AtualizarHospedeRequest request) {
        return hospedeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover hóspede")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Hóspede removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Hóspede não encontrado")
    })
    public void deletar(@PathVariable Long id) {
        hospedeService.deletar(id);
    }
}
