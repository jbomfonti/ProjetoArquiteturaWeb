package com.sistema.gestao.sistemagestao.service;


import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.model.Hospede;
import com.sistema.gestao.sistemagestao.repository.HospedeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;



@ExtendWith(MockitoExtension.class)
public class HospedeServiceTest {
    @Mock
    private HospedeRepository hospedeRepository;

    @InjectMocks
    private HospedeService hospedeService;

    private Hospede hospede;
    private CriarHospedeRequest criarRequest;
    private AtualizarHospedeRequest atualizarRequest;

    @BeforeEach
    void setUp(){
        hospede = new Hospede();
        hospede.setId(1L);
        hospede.setNome("Maria Silva");
        hospede.setEmail("maria@gmail.com");
        hospede.setTelefone("31911111111");
        hospede.setCpf("123.456.789-00");
        hospede.setDataNascimento(LocalDate.of(1999,6,2));

        criarRequest = new CriarHospedeRequest(
                "Maria Silva",
                "maria@gmail.com",
                "31911111111",
                "123.456.789-00",
                LocalDate.of(1999,6,2)
        );

        atualizarRequest = new AtualizarHospedeRequest(
                "Maria Silva Atualizado",
                "maria.novo@gmail.com",
                "31999999999",
                LocalDate.of(2002,3,14)

        );
    }
    @Test
    void deveCriarHospedeComSucesso(){
        when(hospedeRepository.existsByEmail(criarRequest.email())).thenReturn(false);
        when(hospedeRepository.existsByCpf(criarRequest.cpf())).thenReturn(false);

        when(hospedeRepository.save(any(Hospede.class))).thenReturn(hospede);

        HospedeResponse response = hospedeService.criar(criarRequest);

        assertNotNull(response);

        assertEquals("Maria Silva", response.nome());
        assertEquals("maria@gmail.com", response.email());

        verify(hospedeRepository, times(1)).save(any(Hospede.class));
    }
    @Test
    void deveLancarExcecaoQuandoEmailJaCdastradoAoCriar(){
        when(hospedeRepository.existsByEmail(criarRequest.email())).thenReturn(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> hospedeService.criar(criarRequest));
        assertEquals("E-mail já cadastrado", ex.getMessage());
        verify(hospedeRepository, never ()).save(any());
    }
    @Test
    void deveLancarExcecaoQuandoCpfJaCadastradoAoCriar(){
        when(hospedeRepository.existsByEmail(criarRequest.email())).thenReturn(false);
        when(hospedeRepository.existsByCpf(criarRequest.cpf())).thenReturn(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> hospedeService.criar(criarRequest));
        assertEquals("CPF já cadastrado", ex.getMessage());
        verify(hospedeRepository, never ()).save(any());
    }
    @Test
    void deveListarTodoHospede(){
        when(hospedeRepository.findAll()).thenReturn(List.of(hospede));
        List<HospedeResponse> lista = hospedeService.listar();
        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals("Maria Silva", lista.getFirst().nome());
        verify(hospedeRepository, times(1)).findAll();
    }
    @Test
    void deveRetornarListaVaziaQuandoNaoHaHospede(){
        when(hospedeRepository.findAll()).thenReturn(List.of());
        List<HospedeResponse> lista = hospedeService.listar();
        assertNotNull(lista);
        assertTrue(lista.isEmpty());
    }
    @Test
    void deveBuscarHospedePorIdComSucesso(){
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        HospedeResponse response = hospedeService.buscarPorId(1L);
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Maria Silva", response.nome());
    }
    @Test
    void deveLancarExcecaoQuandoHospedeNaoEncontradoPorId(){
        when(hospedeRepository.findById(9L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> hospedeService.buscarPorId(9L));
        assertEquals("Hóspede não encontrado", ex.getMessage());
    }
    @Test
    void deveAtualizarHospedeComSucesso(){
        Hospede hospedeAtualizado = new Hospede();
        hospedeAtualizado.setId(1L);
        hospedeAtualizado.setNome("Maria Silva Atualizado");
        hospedeAtualizado.setEmail("maria.novo@gmail.com");
        hospedeAtualizado.setTelefone("31999999999");
        hospedeAtualizado.setDataNascimento(LocalDate.of(2000,3,14));

        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(hospedeRepository.existsByEmail("maria.novo@gmail.com")).thenReturn(false);
        when(hospedeRepository.save(any(Hospede.class))).thenReturn(hospedeAtualizado);

        HospedeResponse response = hospedeService.atualizar(1L, atualizarRequest);

        assertNotNull(response);
        assertEquals("Maria Silva Atualizado", response.nome());
        assertEquals("maria.novo@gmail.com", response.email());
    }
    @Test
    void deveLancarExcecaoQuandoHospedeNaoEncontradoAoAtualizar(){
        when(hospedeRepository.findById(9L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> hospedeService.atualizar(9L, atualizarRequest));
        assertEquals("Hóspede não encontrado", ex.getMessage());
        verify(hospedeRepository, never()).save(any());
    }
    @Test
    void deveLancarExcecaoQuandoEmailJaUsadoPorOutroHospede(){
        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(hospedeRepository.existsByEmail("maria.novo@gmail.com")).thenReturn(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> hospedeService.atualizar(1L, atualizarRequest));
        assertEquals("E-mail já cadastrado por outro hóspede", ex.getMessage());
        verify(hospedeRepository, never()).save(any());
    }
    @Test
    void deveDeletarHospedeComSucesso(){
        when(hospedeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(hospedeRepository).deleteById(1L);
        assertDoesNotThrow(()-> hospedeService.deletar(1L));
        verify(hospedeRepository, times(1)).deleteById(1L);
    }
    @Test
    void deveLancarExcecaoQuandoHospedeNaoEncontradoAoDeletar(){
        when(hospedeRepository.existsById(9L)).thenReturn(false);
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> hospedeService.deletar(9L));
        assertEquals("Hóspede não encontrado", ex.getMessage());
        verify(hospedeRepository, never()).delete(any());
    }
}
