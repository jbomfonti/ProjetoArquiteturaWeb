# SPEC-006 — Hotel Core: Kafka Consumer

> Depende de: [spec004](spec004-hotelcore-kafka-config.md) (KafkaConfig)

## O que essa spec faz

Escuta o tópico `pagamento.resultado` e atualiza o status da reserva conforme o resultado do pagamento.

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
└── kafka/
    └── consumer/
        └── PagamentoResultadoConsumer.java
```

---

## PagamentoResultadoConsumer.java

```java
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
```

---

## Comportamento por status recebido

| `evento.status` | `reserva.status` resultante |
|---|---|
| `APROVADO` | `CONFIRMADA` |
| `RECUSADO` | `CANCELADA` |
| `ESTORNADO` | `CANCELADA` |

**Retry:** 3 tentativas com 2s de intervalo. Após esgotar, a mensagem vai para `pagamento.resultado.DLT` (visível no Kafka UI em http://localhost:8090).

---

## Fluxo completo do Hotel Core

```
POST /api/reservas
        │
  ReservaService.criarReserva()
        │
        ├── Salva Reserva (PENDENTE)
        └── ReservaProducer ──► reserva.criada
                                        │
                              [Pagamentos Service]
                                        │
                              pagamento.resultado ◄── publica resultado
                                        │
                       PagamentoResultadoConsumer
                                        │
                              Atualiza → CONFIRMADA ou CANCELADA
```

---

**Próxima spec:** [spec007 — Pagamentos Service: Models e Banco](spec007-pagamentos-models.md)
