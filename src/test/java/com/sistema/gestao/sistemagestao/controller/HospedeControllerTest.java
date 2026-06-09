package com.sistema.gestao.sistemagestao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.service.HospedeService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HospedeController.class)
public class HospedeControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private HospedeService hospedeService;
    private ObjectMapper objectMapper;

    private HospedeResponse hospedeResponse;
    private CriarHospedeRequest criarRequest;
    private AtualizarHospedeRequest atualizarRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        hospedeResponse = new HospedeResponse(
                1L,
                "João Silva",
                "joao@email.com",
                "11999999999",
                "123.456.789-00",
                LocalDate.of(1990, 1, 15),
                LocalDateTime.now()
        );

        criarRequest = new CriarHospedeRequest(
                "João Silva",
                "joao@email.com",
                "11999999999",
                "123.456.789-00",
                LocalDate.of(1990, 1, 15)
        );

        atualizarRequest = new AtualizarHospedeRequest(
                "João Silva Atualizado",
                "joao.novo@email.com",
                "11988888888",
                LocalDate.of(1990, 1, 15)
        );
    }
    @Test
    void deveCriarHospedeERetornar201() throws Exception {
        when(hospedeService.criar(any(CriarHospedeRequest.class))).thenReturn(hospedeResponse);

        mockMvc.perform(post("/api/hospedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }
    @Test
    void deveLancarErroAoCriarHospedeComEmailDuplicado() throws Exception {
        when(hospedeService.criar(any(CriarHospedeRequest.class)))
                .thenThrow(new IllegalStateException("E-mail já cadastrado"));

        mockMvc.perform(post("/api/hospedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest)))
                .andExpect(status().isConflict());
    }
    @Test
    void deveListarHospedesERetornar200() throws Exception {
        when(hospedeService.listar()).thenReturn(List.of(hospedeResponse));

        mockMvc.perform(get("/api/hospedes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("João Silva"));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaHospedes() throws Exception {
        when(hospedeService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/hospedes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveBuscarHospedePorIdERetornar200() throws Exception {
        when(hospedeService.buscarPorId(1L)).thenReturn(hospedeResponse);

        mockMvc.perform(get("/api/hospedes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"));
    }
    @Test
    void deveLancarErroAoBuscarHospedeComIdInexistente() throws Exception {
        when(hospedeService.buscarPorId(99L))
                .thenThrow(new EntityNotFoundException("Hóspede não encontrado"));

        mockMvc.perform(get("/api/hospedes/99"))
                .andExpect(status().isNotFound());
    }
    @Test
    void deveAtualizarHospedeERetornar200() throws Exception {
        HospedeResponse hospedeAtualizado = new HospedeResponse(
                1L,
                "João Silva Atualizado",
                "joao.novo@email.com",
                "11988888888",
                "123.456.789-00",
                LocalDate.of(1990, 1, 15),
                LocalDateTime.now()
        );

        when(hospedeService.atualizar(eq(1L), any(AtualizarHospedeRequest.class)))
                .thenReturn(hospedeAtualizado);

        mockMvc.perform(put("/api/hospedes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Silva Atualizado"))
                .andExpect(jsonPath("$.email").value("joao.novo@email.com"));
    }

    @Test
    void deveLancarErroAoAtualizarHospedeInexistente() throws Exception {
        when(hospedeService.atualizar(eq(99L), any(AtualizarHospedeRequest.class)))
                .thenThrow(new EntityNotFoundException("Hóspede não encontrado"));

        mockMvc.perform(put("/api/hospedes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarRequest)))
                .andExpect(status().isNotFound());
    }
    @Test
    void deveDeletarHospedeERetornar204() throws Exception {
        doNothing().when(hospedeService).deletar(1L);

        mockMvc.perform(delete("/api/hospedes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveLancarErroAoDeletarHospedeInexistente() throws Exception {
        doThrow(new EntityNotFoundException("Hóspede não encontrado"))
                .when(hospedeService).deletar(99L);

        mockMvc.perform(delete("/api/hospedes/99"))
                .andExpect(status().isNotFound());
    }
}
