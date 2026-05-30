package com.sistema.gestao.sistemagestao.repository;

import com.sistema.gestao.sistemagestao.model.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ImovelRepository extends JpaRepository<Imovel, Long> {

    List<Imovel> findByDisponivel(Boolean disponivel);

    List<Imovel> findByCapacidadeGreaterThanEqual(Integer capacidadeMinima);

    List<Imovel> findByPrecoPorNoiteLessThanEqual(BigDecimal precoMaximo);

    @Query("""
        SELECT i FROM Imovel i
        WHERE i.disponivel = true
          AND i.capacidade >= :capacidade
          AND i.precoPorNoite <= :precoMaximo
        ORDER BY i.precoPorNoite ASC
    """)
    List<Imovel> buscarDisponiveis(Integer capacidade, BigDecimal precoMaximo);
}
