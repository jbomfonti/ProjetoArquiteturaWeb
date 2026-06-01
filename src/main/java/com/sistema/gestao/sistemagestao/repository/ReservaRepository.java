package com.sistema.gestao.sistemagestao.repository;

import com.sistema.gestao.sistemagestao.model.Reserva;
import com.sistema.gestao.sistemagestao.model.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByHospedeId(Long hospedeId);

    List<Reserva> findByImovelId(Long imovelId);

    List<Reserva> findByStatus(StatusReserva status);

    @Query("""
                SELECT COUNT(r) > 0 FROM Reserva r
                WHERE r.imovel.id = :imovelId
                  AND r.status IN (com.sistema.gestao.sistemagestao.model.StatusReserva.PENDENTE,
                                   com.sistema.gestao.sistemagestao.model.StatusReserva.CONFIRMADA)
                  AND r.dataCheckIn  < :checkOut
                  AND r.dataCheckOut > :checkIn
            """)
    boolean existeConflitoDeDatas(Long imovelId, LocalDate checkIn, LocalDate checkOut);

    @Query("""
                SELECT r FROM Reserva r
                JOIN FETCH r.hospede
                JOIN FETCH r.imovel
                WHERE r.id = :id
            """)
    java.util.Optional<Reserva> findByIdComDetalhes(Long id);
}