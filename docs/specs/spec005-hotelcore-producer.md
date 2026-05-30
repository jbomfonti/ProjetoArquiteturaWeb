# SPEC-005 — Hotel Core: Kafka Producer

> Depende de: [spec003](spec003-hotelcore-endpoints.md) (ReservaService) e [spec004](spec004-hotelcore-kafka-config.md) (KafkaConfig)

## O que essa spec faz

Publica um evento no tópico `reserva.criada` toda vez que uma reserva é criada com status `PENDENTE`.

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
└── kafka/
    └── producer/
        └── ReservaProducer.java
```

---

## ReservaProducer.java

```java
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
```

> **Por que usar `reservaId` como chave?**
> Garante que todos os eventos de uma mesma reserva vão para a mesma partição, preservando a ordem de processamento.

---

## Fluxo

```
ReservaService.criarReserva()
    │
    ├── reservaRepository.save(reserva)    → salva com PENDENTE
    └── reservaProducer.publicar(reserva)  → publica em reserva.criada
                                                     │
                                           [Pagamentos Service consome — spec008]
```

---

**Próxima spec:** [spec006 — Hotel Core: Kafka Consumer](spec006-hotelcore-consumer.md)
