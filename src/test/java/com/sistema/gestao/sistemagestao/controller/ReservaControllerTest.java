package com.sistema.gestao.sistemagestao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.StatusReserva;
import com.sistema.gestao.sistemagestao.service.ReservaService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservaService reservaService;

    private ReservaResponse reservaResponse;
    private CriarReservaRequest criarRequest;

    private final LocalDate checkIn  = LocalDate.now().plusDays(5);
    private final LocalDate checkOut = LocalDate.now().plusDays(8);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        criarRequest = new CriarReservaRequest(1L, 1L, checkIn, checkOut, 2);

        reservaResponse = new ReservaResponse(
                1L,
                1L, "Maria Silva",
                1L, "Casa da Praia",
                checkIn, checkOut,
                2,
                new BigDecimal("900.00"),
                "PENDENTE",
                LocalDateTime.now()
        );
    }

    // ---------------------------------------------------------------
    // POST /api/reservas — criar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /api/reservas — deve retornar 201 com a reserva criada")
    void deveCriarReserva() throws Exception {
        when(reservaService.criar(any())).thenReturn(reservaResponse);

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nomeHospede").value("Maria Silva"))
                .andExpect(jsonPath("$.nomeImovel").value("Casa da Praia"))
                .andExpect(jsonPath("$.valorTotal").value(900.00))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /api/reservas — deve retornar 400 quando hospedeId é null")
    void deveRetornar400QuandoHospedeIdNulo() throws Exception {
        String jsonInvalido = """
                {
                    "hospedeId": null,
                    "imovelId": 1,
                    "dataCheckIn": "%s",
                    "dataCheckOut": "%s",
                    "numeroHospedes": 2
                }
                """.formatted(checkIn, checkOut);

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reservas — deve retornar 400 quando numeroHospedes é zero")
    void deveRetornar400QuandoNumeroHospedesZero() throws Exception {
        String jsonInvalido = """
                {
                    "hospedeId": 1,
                    "imovelId": 1,
                    "dataCheckIn": "%s",
                    "dataCheckOut": "%s",
                    "numeroHospedes": 0
                }
                """.formatted(checkIn, checkOut);

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // GET /api/reservas — listar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/reservas — deve retornar 200 com lista de reservas")
    void deveListarReservas() throws Exception {
        when(reservaService.listar(null)).thenReturn(List.of(reservaResponse));

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomeHospede").value("Maria Silva"));
    }

    @Test
    @DisplayName("GET /api/reservas?hospedeId=1 — deve filtrar por hóspede")
    void deveListarReservasPorHospede() throws Exception {
        when(reservaService.listar(1L)).thenReturn(List.of(reservaResponse));

        mockMvc.perform(get("/api/reservas").param("hospedeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hospedeId").value(1));
    }

    @Test
    @DisplayName("GET /api/reservas — deve retornar lista vazia")
    void deveRetornarListaVazia() throws Exception {
        when(reservaService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------
    // GET /api/reservas/{id} — buscar por ID
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /api/reservas/{id} — deve retornar 200 com a reserva")
    void deveBuscarReservaPorId() throws Exception {
        when(reservaService.buscarPorId(1L)).thenReturn(reservaResponse);

        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("GET /api/reservas/{id} — deve lançar exceção quando não encontrada")
    void deveLancarExcecaoQuandoReservaNaoExiste() throws Exception {
        when(reservaService.buscarPorId(99L))
                .thenThrow(new EntityNotFoundException("Reserva não encontrada"));

        try {
            mockMvc.perform(get("/api/reservas/99"));
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof EntityNotFoundException);
            assertTrue(ex.getCause().getMessage().contains("Reserva não encontrada"));
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/reservas/{id}/status — atualizar status
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/reservas/{id}/status — deve atualizar para CONFIRMADA")
    void deveAtualizarStatusParaConfirmada() throws Exception {
        ReservaResponse confirmada = new ReservaResponse(
                1L, 1L, "Maria Silva", 1L, "Casa da Praia",
                checkIn, checkOut, 2, new BigDecimal("900.00"),
                "CONFIRMADA", LocalDateTime.now()
        );

        AtualizarStatusReservaRequest request =
                new AtualizarStatusReservaRequest(StatusReserva.CONFIRMADA);

        when(reservaService.atualizarStatus(eq(1L), any())).thenReturn(confirmada);

        mockMvc.perform(patch("/api/reservas/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/status — deve retornar 400 quando status é null")
    void deveRetornar400QuandoStatusNulo() throws Exception {
        mockMvc.perform(patch("/api/reservas/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // DELETE /api/reservas/{id} — deletar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/reservas/{id} — deve retornar 204 ao deletar com sucesso")
    void deveDeletarReserva() throws Exception {
        doNothing().when(reservaService).deletar(1L);

        mockMvc.perform(delete("/api/reservas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id} — deve lançar exceção quando não existe")
    void deveLancarExcecaoAoDeletarReservaInexistente() throws Exception {
        doThrow(new EntityNotFoundException("Reserva não encontrada"))
                .when(reservaService).deletar(99L);

        try {
            mockMvc.perform(delete("/api/reservas/99"));
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof EntityNotFoundException);
            assertTrue(ex.getCause().getMessage().contains("Reserva não encontrada"));
        }
    }
}  