package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.Imovel;
import com.sistema.gestao.sistemagestao.repository.ImovelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) ativa o Mockito para criar os mocks automaticamente
@ExtendWith(MockitoExtension.class)
class ImovelServiceTest {

    // @Mock cria um "dublê" do repositório — sem banco de dados real
    @Mock
    private ImovelRepository imovelRepository;

    // @InjectMocks cria o service e injeta o mock acima nele
    @InjectMocks
    private ImovelService imovelService;

    // Objetos reutilizados entre os testes
    private Imovel imovelSalvo;
    private CriarImovelRequest criarRequest;
    private AtualizarImovelRequest atualizarRequest;

    // @BeforeEach roda antes de CADA teste para preparar os dados
    @BeforeEach
    void setUp() {
        criarRequest = new CriarImovelRequest(
                "Casa da Praia",
                "Linda casa à beira-mar",
                "Rua das Flores, 100",
                new BigDecimal("350.00"),
                6
        );

        imovelSalvo = new Imovel();
        imovelSalvo.setNome("Casa da Praia");
        imovelSalvo.setDescricao("Linda casa à beira-mar");
        imovelSalvo.setEndereco("Rua das Flores, 100");
        imovelSalvo.setPrecoPorNoite(new BigDecimal("350.00"));
        imovelSalvo.setCapacidade(6);
        imovelSalvo.setDisponivel(true);
        // Simulamos o que o @PrePersist faria no banco
        setId(imovelSalvo, 1L);

        atualizarRequest = new AtualizarImovelRequest(
                "Casa da Praia Atualizada",
                "Descrição nova",
                "Rua Nova, 200",
                new BigDecimal("400.00"),
                8
        );
    }

    // ---------------------------------------------------------------
    // TESTES: criar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve criar imóvel e retornar response com os dados corretos")
    void deveCriarImovel() {
        // GIVEN — configuramos o que o repositório deve retornar ao salvar
        when(imovelRepository.save(any(Imovel.class))).thenReturn(imovelSalvo);

        // WHEN — chamamos o método que queremos testar
        ImovelResponse response = imovelService.criar(criarRequest);

        // THEN — verificamos o resultado
        assertThat(response).isNotNull();
        assertThat(response.nome()).isEqualTo("Casa da Praia");
        assertThat(response.precoPorNoite()).isEqualByComparingTo("350.00");
        assertThat(response.disponivel()).isTrue();

        // Verifica que o repositório foi chamado exatamente 1 vez
        verify(imovelRepository, times(1)).save(any(Imovel.class));
    }

    // ---------------------------------------------------------------
    // TESTES: listar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve listar todos os imóveis quando disponivel é null")
    void deveListarTodosImoveis() {
        when(imovelRepository.findAll()).thenReturn(List.of(imovelSalvo));

        List<ImovelResponse> lista = imovelService.listar(null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Casa da Praia");
        verify(imovelRepository).findAll();
        verify(imovelRepository, never()).findByDisponivel(any());
    }

    @Test
    @DisplayName("deve listar apenas imóveis disponíveis quando disponivel=true")
    void deveListarImoveisDisponiveis() {
        when(imovelRepository.findByDisponivel(true)).thenReturn(List.of(imovelSalvo));

        List<ImovelResponse> lista = imovelService.listar(true);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).disponivel()).isTrue();
        verify(imovelRepository).findByDisponivel(true);
        verify(imovelRepository, never()).findAll();
    }

    @Test
    @DisplayName("deve retornar lista vazia quando não há imóveis disponíveis")
    void deveRetornarListaVaziaQuandoNaoHaDisponiveis() {
        when(imovelRepository.findByDisponivel(true)).thenReturn(List.of());

        List<ImovelResponse> lista = imovelService.listar(true);

        assertThat(lista).isEmpty();
    }

    // ---------------------------------------------------------------
    // TESTES: buscarPorId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve retornar imóvel quando ID existe")
    void deveBuscarImovelPorId() {
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovelSalvo));

        ImovelResponse response = imovelService.buscarPorId(1L);

        assertThat(response.nome()).isEqualTo("Casa da Praia");
    }

    @Test
    @DisplayName("deve lançar EntityNotFoundException quando ID não existe")
    void deveLancarExcecaoQuandoImovelNaoEncontrado() {
        when(imovelRepository.findById(99L)).thenReturn(Optional.empty());

        // assertThatThrownBy verifica que o método lança a exceção esperada
        assertThatThrownBy(() -> imovelService.buscarPorId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Imóvel não encontrado");
    }

    // ---------------------------------------------------------------
    // TESTES: atualizar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve atualizar imóvel com os novos dados")
    void deveAtualizarImovel() {
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovelSalvo));
        when(imovelRepository.save(any())).thenReturn(imovelSalvo);

       assertThatCode(() -> imovelService.atualizar(1L, atualizarRequest))
        .doesNotThrowAnyException();

        // Verifica que os dados foram alterados no objeto antes de salvar
        verify(imovelRepository).save(argThat(imovel ->
                imovel.getNome().equals("Casa da Praia Atualizada") &&
                imovel.getCapacidade() == 8
        ));
    }

    @Test
    @DisplayName("deve lançar exceção ao tentar atualizar imóvel inexistente")
    void deveLancarExcecaoAoAtualizarImovelInexistente() {
        when(imovelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imovelService.atualizar(99L, atualizarRequest))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // TESTES: alterarDisponibilidade()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve alterar disponibilidade do imóvel para false")
    void deveAlterarDisponibilidadeParaFalse() {
        AlterarDisponibilidadeRequest request = new AlterarDisponibilidadeRequest(false);
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovelSalvo));
        when(imovelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImovelResponse response = imovelService.alterarDisponibilidade(1L, request);

        assertThat(response.disponivel()).isFalse();
    }

    // ---------------------------------------------------------------
    // TESTES: deletar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve deletar imóvel existente sem erros")
    void deveDeletarImovel() {
        when(imovelRepository.existsById(1L)).thenReturn(true);

        // Não lança exceção = passou!
        assertThatCode(() -> imovelService.deletar(1L)).doesNotThrowAnyException();

        verify(imovelRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deve lançar exceção ao tentar deletar imóvel inexistente")
    void deveLancarExcecaoAoDeletarImovelInexistente() {
        when(imovelRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> imovelService.deletar(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Imóvel não encontrado");

        verify(imovelRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------
    // Utilitário: seta o ID via reflection (pois não há setId em uso aqui)
    // ---------------------------------------------------------------
    private void setId(Imovel imovel, Long id) {
        try {
            var field = Imovel.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(imovel, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}