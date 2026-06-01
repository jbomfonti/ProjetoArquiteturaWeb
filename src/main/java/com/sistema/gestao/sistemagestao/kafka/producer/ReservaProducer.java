package com.sistema.gestao.sistemagestao.kafka.producer;

import com.sistema.gestao.sistemagestao.event.ReservaCriadaEvent;
import com.sistema.gestao.sistemagestao.model.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservaProducer {

    private static final Logger log = LoggerFactory.getLogger(ReservaProducer.class);

    @Value("${kafka.topics.reserva-criada}")
    private String topico;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReservaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(Reserva reserva) {
        ReservaCriadaEvent evento = new ReservaCriadaEvent(
                reserva.getId(),
                reserva.getHospede().getId(),
                reserva.getImovel().getId(),
                reserva.getDataCheckIn(),
                reserva.getDataCheckOut(),
                reserva.getValorTotal()
        );

        kafkaTemplate.send(topico, reserva.getId().toString(), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar reserva.criada reservaId={}", reserva.getId(), ex);
                    } else {
                        log.info("reserva.criada publicado reservaId={} partition={} offset={}",
                                reserva.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}
