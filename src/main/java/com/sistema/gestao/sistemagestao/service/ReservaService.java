package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.*;
import com.sistema.gestao.sistemagestao.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HospedeRepository hospedeRepository;
    private final ImovelRepository imovelRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          HospedeRepository hospedeRepository,
                          ImovelRepository imovelRepository) {
        this.reservaRepository = reservaRepository;
        this.hospedeRepository = hospedeRepository;
        this.imovelRepository = imovelRepository;
    }

    public ReservaResponse criar(CriarReservaRequest request) {
        if (!request.dataCheckOut().isAfter(request.dataCheckIn()))
            throw new IllegalArgumentException("Data de check-out deve ser posterior ao check-in");

        Hospede hospede = hospedeRepository.findById(request.hospedeId())
                .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));

        Imovel imovel = imovelRepository.findById(request.imovelId())
                .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));

        if (!imovel.getDisponivel())
            throw new IllegalStateException("Imóvel indisponível");

        if (reservaRepository.existeConflitoDeDatas(
                imovel.getId(), request.dataCheckIn(), request.dataCheckOut()))
            throw new IllegalStateException("Imóvel já reservado para o período solicitado");

        long noites = ChronoUnit.DAYS.between(request.dataCheckIn(), request.dataCheckOut());
        BigDecimal valorTotal = imovel.getPrecoPorNoite().multiply(BigDecimal.valueOf(noites));

        Reserva reserva = new Reserva();
        reserva.setHospede(hospede);
        reserva.setImovel(imovel);
        reserva.setDataCheckIn(request.dataCheckIn());
        reserva.setDataCheckOut(request.dataCheckOut());
        reserva.setNumeroHospedes(request.numeroHospedes());
        reserva.setValorTotal(valorTotal);

        return toResponse(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(Long hospedeId) {
        List<Reserva> reservas = (hospedeId != null)
                ? reservaRepository.findByHospedeId(hospedeId)
                : reservaRepository.findAll();
        return reservas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
    }

    public ReservaResponse atualizarStatus(Long id, AtualizarStatusReservaRequest request) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
        reserva.setStatus(request.status());
        return toResponse(reservaRepository.save(reserva));
    }

    public void deletar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
        if (reserva.getStatus() == StatusReserva.CONFIRMADA)
            throw new IllegalStateException("Não é possível remover reserva já confirmada — use PATCH /status para cancelar");
        reservaRepository.deleteById(id);
    }

    private ReservaResponse toResponse(Reserva r) {
        return new ReservaResponse(
                r.getId(),
                r.getHospede().getId(), r.getHospede().getNome(),
                r.getImovel().getId(), r.getImovel().getNome(),
                r.getDataCheckIn(), r.getDataCheckOut(),
                r.getNumeroHospedes(), r.getValorTotal(),
                r.getStatus().name(), r.getCriadoEm()
        );
    }
}