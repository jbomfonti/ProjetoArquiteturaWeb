# SPEC-005 — Hotel Core: Kafka Producer

---

## Imagens Docker

| Serviço    | Imagem                    | Versão  |
|------------|---------------------------|---------|
| PostgreSQL | `postgres`                | `16`    |
| Kafka      | `confluentinc/cp-kafka`   | `7.6.0` |
| Zookeeper  | `confluentinc/cp-zookeeper` | `7.6.0` |

> Infraestrutura completa em [spec001](spec001-infraestrutura-docker.md).

---

## Objetivo

Implementar o **producer Kafka** do Hotel Core que publica no tópico `reserva.criada` toda vez que uma nova reserva é salva com status `PENDENTE`.

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
└── kafka/
    └── producer/
        └── ReservaProducer.java
```

---

## Implementação

### ReservaProducer.java

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

        // chave = reservaId garante ordem na mesma partição
        String chave = reserva.getId().toString();

        kafkaTemplate.send(topico, chave, evento)
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

---

## Por que usar `reservaId` como chave?

A chave de partição determina em qual partição a mensagem cai.  
Usando `reservaId.toString()` como chave:

- Todos os eventos de uma mesma reserva sempre vão para a **mesma partição**
- A **ordem de processamento** é preservada por reserva
- Evita condições de corrida entre `reserva.criada` e `pagamento.resultado` da mesma reserva

---

## Fluxo após publicação

```
ReservaService.criarReserva()
        │
        ├── reservaRepository.save(reserva)   ← salva com PENDENTE
        │
        └── reservaProducer.publicar(reserva) ── publica ──► reserva.criada
                                                                     │
                                                              [Pagamentos Service consome]
                                                              [ver spec008]
```

---

**Próxima spec:** [spec006 — Hotel Core: Kafka Consumer](spec006-hotelcore-consumer.md)
