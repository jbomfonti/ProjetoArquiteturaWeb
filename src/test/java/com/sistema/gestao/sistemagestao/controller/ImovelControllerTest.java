package com.sistema.gestao.sistemagestao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ImovelService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImovelController.class)
class ImovelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private ImovelService imovelService;

    private ImovelResponse imovelResponse;
    private CriarImovelRequest criarRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        criarRequest = new CriarImovelRequest(
                "Casa da Praia",
                "Linda casa à beira-mar",
                "Rua das Flores, 100",
                new BigDecimal("350.00"),
                6
        );

        imovelResponse = new ImovelResponse(
                1L,
                "Casa da Praia",
                "Linda casa à beira-mar",
                "Rua das Flores, 100",
                new BigDecimal("350.00"),
                6,
                true,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/imoveis — deve retornar 201 e o imóvel criado")
    void deveCriarImovel() throws Exception {
        when(imovelService.criar(any())).thenReturn(imovelResponse);

        mockMvc.perform(post("/api/imoveis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Casa da Praia"))
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.precoPorNoite").value(350.00));
    }

    @Test
    @DisplayName("POST /api/imoveis — deve retornar 400 quando campos obrigatórios faltam")
    void deveRetornar400QuandoCriarComDadosInvalidos() throws Exception {
        String jsonInvalido = """
                {
                    "nome": "",
                    "descricao": "Descrição",
                    "endereco": "Rua X",
                    "precoPorNoite": -10,
                    "capacidade": 0
                }
                """;

        mockMvc.perform(post("/api/imoveis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/imoveis — deve retornar 200 com lista de imóveis")
    void deveListarImoveis() throws Exception {
        when(imovelService.listar(null)).thenReturn(List.of(imovelResponse));

        mockMvc.perform(get("/api/imoveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Casa da Praia"));
    }

    @Test
    @DisplayName("GET /api/imoveis?disponivel=true — deve filtrar por disponibilidade")
    void deveListarImoveisDisponiveis() throws Exception {
        when(imovelService.listar(true)).thenReturn(List.of(imovelResponse));

        mockMvc.perform(get("/api/imoveis").param("disponivel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponivel").value(true));
    }

    @Test
    @DisplayName("GET /api/imoveis — deve retornar lista vazia quando não há imóveis")
    void deveRetornarListaVazia() throws Exception {
        when(imovelService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/imoveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/imoveis/{id} — deve retornar 200 com o imóvel")
    void deveBuscarImovelPorId() throws Exception {
        when(imovelService.buscarPorId(1L)).thenReturn(imovelResponse);

        mockMvc.perform(get("/api/imoveis/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Casa da Praia"));
    }

    @Test
    @DisplayName("GET /api/imoveis/{id} — deve lançar exceção quando não encontrado")
    void deveRetornar404QuandoImovelNaoExiste() throws Exception {
        when(imovelService.buscarPorId(99L))
                .thenThrow(new EntityNotFoundException("Imóvel não encontrado"));

        try {
            mockMvc.perform(get("/api/imoveis/99"));
        } catch (Exception ex) {
            assertTrue(
                ex.getCause() instanceof EntityNotFoundException,
                "Esperava EntityNotFoundException"
            );
        }
    }

    @Test
    @DisplayName("PUT /api/imoveis/{id} — deve retornar 200 com dados atualizados")
    void deveAtualizarImovel() throws Exception {
        AtualizarImovelRequest atualizarRequest = new AtualizarImovelRequest(
                "Casa Nova",
                "Nova descrição",
                "Rua Nova, 200",
                new BigDecimal("450.00"),
                8
        );

        ImovelResponse responseAtualizado = new ImovelResponse(
                1L, "Casa Nova", "Nova descrição", "Rua Nova, 200",
                new BigDecimal("450.00"), 8, true, LocalDateTime.now()
        );

        when(imovelService.atualizar(eq(1L), any())).thenReturn(responseAtualizado);

        mockMvc.perform(put("/api/imoveis/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Casa Nova"))
                .andExpect(jsonPath("$.capacidade").value(8));
    }

    @Test
    @DisplayName("PATCH /api/imoveis/{id}/disponibilidade — deve alterar para false")
    void deveAlterarDisponibilidadeParaFalse() throws Exception {
        AlterarDisponibilidadeRequest request = new AlterarDisponibilidadeRequest(false);

        ImovelResponse indisponivel = new ImovelResponse(
                1L, "Casa da Praia", "Descrição", "Rua X",
                new BigDecimal("350.00"), 6, false, LocalDateTime.now()
        );

        when(imovelService.alterarDisponibilidade(eq(1L), any())).thenReturn(indisponivel);

        mockMvc.perform(patch("/api/imoveis/1/disponibilidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(false));
    }

    @Test
    @DisplayName("PATCH /api/imoveis/{id}/disponibilidade — deve retornar 400 se disponivel for null")
    void deveRetornar400SeDisponibilidadeForNull() throws Exception {
        mockMvc.perform(patch("/api/imoveis/1/disponibilidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponivel\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/imoveis/{id} — deve retornar 204 ao deletar com sucesso")
    void deveDeletarImovel() throws Exception {
        doNothing().when(imovelService).deletar(1L);

        mockMvc.perform(delete("/api/imoveis/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/imoveis/{id} — deve lançar exceção quando não existe")
    void deveRetornar404AoDeletarImovelInexistente() throws Exception {
        doThrow(new EntityNotFoundException("Imóvel não encontrado"))
                .when(imovelService).deletar(99L);

        try {
            mockMvc.perform(delete("/api/imoveis/99"));
        } catch (Exception ex) {
            assertTrue(
                ex.getCause() instanceof EntityNotFoundException,
                "Esperava EntityNotFoundException"
            );
        }
    }
}