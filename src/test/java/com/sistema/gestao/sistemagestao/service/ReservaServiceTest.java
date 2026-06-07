/*package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.kafka.producer.ReservaProducer;
import com.sistema.gestao.sistemagestao.model.*;
import com.sistema.gestao.sistemagestao.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private HospedeRepository hospedeRepository;

    @Mock
    private ImovelRepository imovelRepository;

    @Mock
    private ReservaProducer reservaProducer;

    @InjectMocks
    private ReservaService reservaService;

    // Datas futuras para não violar @FutureOrPresent
    private final LocalDate checkIn  = LocalDate.now().plusDays(5);
    private final LocalDate checkOut = LocalDate.now().plusDays(8); // 3 noites

    private Hospede hospede;
    private Imovel  imovel;
    private Reserva reserva;
    private CriarReservaRequest criarRequest;

    @BeforeEach
    void setUp() {
        hospede = new Hospede();
        hospede.setNome("Maria Silva");
        setId(hospede, Hospede.class, 1L);

        imovel = new Imovel();
        imovel.setNome("Casa da Praia");
        imovel.setPrecoPorNoite(new BigDecimal("300.00"));
        imovel.setDisponivel(true);
        setId(imovel, Imovel.class, 1L);

        criarRequest = new CriarReservaRequest(1L, 1L, checkIn, checkOut, 2);

        reserva = new Reserva();
        reserva.setHospede(hospede);
        reserva.setImovel(imovel);
        reserva.setDataCheckIn(checkIn);
        reserva.setDataCheckOut(checkOut);
        reserva.setNumeroHospedes(2);
        reserva.setValorTotal(new BigDecimal("900.00")); // 3 noites × 300
        reserva.setStatus(StatusReserva.PENDENTE);
        setField(reserva, Reserva.class, "criadoEm", LocalDateTime.now());
        setId(reserva, Reserva.class, 1L);
    }

    // ---------------------------------------------------------------
    // TESTES: criar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve criar reserva e calcular valor total corretamente")
    void deveCriarReservaECalcularValorTotal() {
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any())).thenReturn(reserva);

        ReservaResponse response = reservaService.criar(criarRequest);

        // 3 noites × R$300 = R$900
        assertThat(response.valorTotal()).isEqualByComparingTo("900.00");
        assertThat(response.status()).isEqualTo("PENDENTE");

        // Verifica que o evento Kafka foi publicado
        verify(reservaProducer, times(1)).publicar(any(Reserva.class));
    }

    @Test
    @DisplayName("deve lançar exceção quando checkout é antes do checkin")
    void deveLancarExcecaoQuandoDatasInvalidas() {
        CriarReservaRequest requestInvalida = new CriarReservaRequest(
                1L, 1L,
                checkIn,
                checkIn, // checkout == checkin — inválido
                2
        );

        assertThatThrownBy(() -> reservaService.criar(requestInvalida))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("check-out deve ser posterior");

        // Nunca deve chegar ao banco
        verifyNoInteractions(reservaRepository);
    }

    @Test
    @DisplayName("deve lançar exceção quando hóspede não existe")
    void deveLancarExcecaoQuandoHospedeNaoExiste() {
        when(hospedeRepository.findById(99L)).thenReturn(Optional.empty());

        CriarReservaRequest request = new CriarReservaRequest(99L, 1L, checkIn, checkOut, 2);

        assertThatThrownBy(() -> reservaService.criar(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hóspede não encontrado");
    }

    @Test
    @DisplayName("deve lançar exceção quando imóvel não existe")
    void deveLancarExcecaoQuandoImovelNaoExiste() {
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(99L)).thenReturn(Optional.empty());

        CriarReservaRequest request = new CriarReservaRequest(1L, 99L, checkIn, checkOut, 2);

        assertThatThrownBy(() -> reservaService.criar(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Imóvel não encontrado");
    }

    @Test
    @DisplayName("deve lançar exceção quando imóvel está indisponível")
    void deveLancarExcecaoQuandoImovelIndisponivel() {
        imovel.setDisponivel(false);
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));

        assertThatThrownBy(() -> reservaService.criar(criarRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("indisponível");
    }

    @Test
    @DisplayName("deve lançar exceção quando há conflito de datas")
    void deveLancarExcecaoQuandoHaConflitoDeDatas() {
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservaService.criar(criarRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já reservado para o período");
    }

    @Test
    @DisplayName("deve publicar evento no Kafka ao criar reserva")
    void devePublicarEventoKafkaAoCriarReserva() {
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any())).thenReturn(reserva);

        reservaService.criar(criarRequest);

        verify(reservaProducer).publicar(reserva);
    }

    // ---------------------------------------------------------------
    // TESTES: listar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve listar todas as reservas quando hospedeId é null")
    void deveListarTodasReservas() {
        when(reservaRepository.findAll()).thenReturn(List.of(reserva));

        List<ReservaResponse> lista = reservaService.listar(null);

        assertThat(lista).hasSize(1);
        verify(reservaRepository).findAll();
        verify(reservaRepository, never()).findByHospedeId(any());
    }

    @Test
    @DisplayName("deve listar reservas filtradas por hóspede")
    void deveListarReservasPorHospede() {
        when(reservaRepository.findByHospedeId(1L)).thenReturn(List.of(reserva));

        List<ReservaResponse> lista = reservaService.listar(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).hospedeId()).isEqualTo(1L);
        verify(reservaRepository).findByHospedeId(1L);
        verify(reservaRepository, never()).findAll();
    }

    // ---------------------------------------------------------------
    // TESTES: buscarPorId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve retornar reserva quando ID existe")
    void deveBuscarReservaPorId() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        ReservaResponse response = reservaService.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nomeHospede()).isEqualTo("Maria Silva");
        assertThat(response.nomeImovel()).isEqualTo("Casa da Praia");
    }

    @Test
    @DisplayName("deve lançar exceção quando reserva não encontrada")
    void deveLancarExcecaoQuandoReservaNaoEncontrada() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.buscarPorId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Reserva não encontrada");
    }

    // ---------------------------------------------------------------
    // TESTES: atualizarStatus()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve atualizar status da reserva para CONFIRMADA")
    void deveAtualizarStatusParaConfirmada() {
        AtualizarStatusReservaRequest request =
                new AtualizarStatusReservaRequest(StatusReserva.CONFIRMADA);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setStatus(StatusReserva.CONFIRMADA);
            return r;
        });

        ReservaResponse response = reservaService.atualizarStatus(1L, request);

        assertThat(response.status()).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar status de reserva inexistente")
    void deveLancarExcecaoAoAtualizarStatusDeReservaInexistente() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.atualizarStatus(
                99L, new AtualizarStatusReservaRequest(StatusReserva.CONFIRMADA)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Reserva não encontrada");
    }

    // ---------------------------------------------------------------
    // TESTES: deletar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve deletar reserva com status PENDENTE")
    void deveDeletarReservaPendente() {
        reserva.setStatus(StatusReserva.PENDENTE);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThatCode(() -> reservaService.deletar(1L)).doesNotThrowAnyException();

        verify(reservaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deve lançar exceção ao tentar deletar reserva CONFIRMADA")
    void deveLancarExcecaoAoDeletarReservaConfirmada() {
        reserva.setStatus(StatusReserva.CONFIRMADA);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.deletar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não é possível remover reserva já confirmada");

        verify(reservaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao deletar reserva inexistente")
    void deveLancarExcecaoAoDeletarReservaInexistente() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.deletar(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Reserva não encontrada");
    }

    // ---------------------------------------------------------------
    // Utilitários de reflection
    // ---------------------------------------------------------------
    private <T> void setId(T obj, Class<T> clazz, Long id) {
        setField(obj, clazz, "id", id);
    }

    private <T> void setField(T obj, Class<T> clazz, String fieldName, Object value) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}*/