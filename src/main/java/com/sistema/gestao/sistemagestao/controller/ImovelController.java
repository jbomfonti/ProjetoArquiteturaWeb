package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ImovelService;
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
@RequestMapping("/api/imoveis")
@Validated
@Tag(name = "Imóveis", description = "Gerenciamento de imóveis e quartos")
public class ImovelController {

    private final ImovelService imovelService;

    public ImovelController(ImovelService imovelService) {
        this.imovelService = imovelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar imóvel", description = "Cria um novo imóvel ou quarto no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imóvel criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ImovelResponse criar(@RequestBody @Valid CriarImovelRequest request) {
        return imovelService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Listar imóveis", description = "Lista imóveis com filtro opcional por disponibilidade")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public List<ImovelResponse> listar(@RequestParam(required = false) Boolean disponivel) {
        return imovelService.listar(disponivel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar imóvel por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imóvel encontrado"),
            @ApiResponse(responseCode = "404", description = "Imóvel não encontrado")
    })
    public ImovelResponse buscar(@PathVariable Long id) {
        return imovelService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do imóvel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imóvel atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Imóvel não encontrado")
    })
    public ImovelResponse atualizar(@PathVariable Long id,
                                    @RequestBody @Valid AtualizarImovelRequest request) {
        return imovelService.atualizar(id, request);
    }

    @PatchMapping("/{id}/disponibilidade")
    @Operation(summary = "Alterar disponibilidade do imóvel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidade atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Imóvel não encontrado")
    })
    public ImovelResponse alterarDisponibilidade(@PathVariable Long id,
                                                 @RequestBody @Valid AlterarDisponibilidadeRequest request) {
        return imovelService.alterarDisponibilidade(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover imóvel")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imóvel removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Imóvel não encontrado")
    })
    public void deletar(@PathVariable Long id) {
        imovelService.deletar(id);
    }
}
