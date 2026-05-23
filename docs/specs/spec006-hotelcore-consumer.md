# SPEC-006 — Hotel Core: Kafka Consumer

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

Implementar o **consumer Kafka** do Hotel Core que escuta o tópico `pagamento.resultado` e atualiza o status da reserva de acordo com o resultado do pagamento.

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
└── kafka/
    └── consumer/
        └── PagamentoResultadoConsumer.java
```

---

## Implementação

### PagamentoResultadoConsumer.java

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
            .orElseThrow(() -> new EntityNotFoundException(
                "Reserva não encontrada: " + evento.reservaId()
            ));

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
|-----------------|-----------------------------|
| `APROVADO`      | `CONFIRMADA`                |
| `RECUSADO`      | `CANCELADA`                 |
| `ESTORNADO`     | `CANCELADA`                 |

---

## Retry e Dead Letter Topic

Configurado em `KafkaConfig` ([spec004](spec004-hotelcore-kafka-config.md)):

- **3 tentativas** com intervalo de **2 segundos**
- Após esgotar, a mensagem vai para `pagamento.resultado.DLT`
- O DLT pode ser monitorado pelo Kafka UI em `http://localhost:8090`

---

## Fluxo completo do Hotel Core

```
POST /api/reservas
        │
        ▼
  ReservaService.criarReserva()
        │
        ├── Salva Reserva (PENDENTE)
        │
        └── ReservaProducer ──► reserva.criada
                                        │
                              [Pagamentos Service]
                                        │
                              pagamento.resultado ◄── publica
                                        │
                                        ▼
                       PagamentoResultadoConsumer
                                        │
                              Atualiza Reserva
                              → CONFIRMADA ou CANCELADA
```

---

**Próxima spec:** [spec007 — Pagamentos Service: Models e Banco](spec007-pagamentos-models.md)
