package com.sistema.gestao.sistemagestao.kafka.consumer;

import com.sistema.gestao.sistemagestao.event.PagamentoResultadoEvent;
import com.sistema.gestao.sistemagestao.model.Reserva;
import com.sistema.gestao.sistemagestao.model.StatusReserva;
import com.sistema.gestao.sistemagestao.repository.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PagamentoResultadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoResultadoConsumer.class);

    private final ReservaRepository reservaRepository;

    public PagamentoResultadoConsumer(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @KafkaListener(
            topics = "${kafka.topics.pagamento-resultado}",
            groupId = "hotel-core-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumir(PagamentoResultadoEvent evento) {
        log.info("pagamento.resultado recebido reservaId={} status={}", evento.reservaId(), evento.status());

        Reserva reserva = reservaRepository.findById(evento.reservaId())
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada: " + evento.reservaId()));

        StatusReserva novoStatus = switch (evento.status()) {
            case "APROVADO"  -> StatusReserva.CONFIRMADA;
            case "RECUSADO"  -> StatusReserva.CANCELADA;
            case "ESTORNADO" -> StatusReserva.CANCELADA;
            default -> throw new IllegalArgumentException("Status desconhecido: " + evento.status());
        };

        reserva.setStatus(novoStatus);
        reservaRepository.save(reserva);

        log.info("Reserva {} atualizada para {}", evento.reservaId(), novoStatus);
    }
}
