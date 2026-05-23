# SPEC-008 — Pagamentos Service: Consumer e Gateway

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

Implementar no **Pagamentos Service**:

1. `ReservaCriadaConsumer` — consome `reserva.criada`, processa o pagamento via gateway e persiste o resultado
2. `GatewayPagamento` — interface do gateway externo
3. `MockGatewayPagamento` — implementação mock para desenvolvimento/testes

---

## Estrutura de arquivos

```
pagamentos-service/src/main/java/com/hotel/pagamentos/
├── kafka/
│   └── consumer/
│       └── ReservaCriadaConsumer.java
├── gateway/
│   ├── GatewayPagamento.java
│   └── MockGatewayPagamento.java
├── service/
│   └── PagamentoService.java
└── event/
    ├── ReservaCriadaEvent.java
    └── PagamentoResultadoEvent.java
```

---

## Implementação

### ReservaCriadaEvent.java (cópia local do evento do Hotel Core)

```java
// O Pagamentos Service mantém sua própria cópia do evento — sem dependência de JAR compartilhado
public record ReservaCriadaEvent(
    Long reservaId,
    Long hospedeId,
    Long imovelId,
    LocalDate dataCheckIn,
    LocalDate dataCheckOut,
    BigDecimal valorTotal
) {}
```

---

### GatewayPagamento.java (interface)

```java
public interface GatewayPagamento {

    ResultadoGateway processar(Long reservaId, BigDecimal valor);

    record ResultadoGateway(boolean aprovado, String motivo) {}
}
```

---

### MockGatewayPagamento.java

```java
@Component
@Primary
public class MockGatewayPagamento implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(MockGatewayPagamento.class);

    // aprova 80% das transações — simula comportamento realista
    @Override
    public ResultadoGateway processar(Long reservaId, BigDecimal valor) {
        log.info("Gateway mock processando reservaId={} valor={}", reservaId, valor);

        boolean aprovado = Math.random() > 0.2;
        String motivo = aprovado ? null : "Limite insuficiente (mock)";

        return new ResultadoGateway(aprovado, motivo);
    }
}
```

---

### PagamentoService.java

```java
@Service
@Transactional
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final GatewayPagamento gateway;
    private final PagamentoResultadoProducer resultadoProducer;

    public void processar(ReservaCriadaEvent evento) {
        // idempotência: mensagem duplicada do Kafka é ignorada
        if (pagamentoRepository.existsByReservaId(evento.reservaId())) {
            log.warn("Pagamento já processado para reservaId={}, ignorando", evento.reservaId());
            return;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setReservaId(evento.reservaId());
        pagamento.setValor(evento.valorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamentoRepository.save(pagamento);

        GatewayPagamento.ResultadoGateway resultado =
            gateway.processar(evento.reservaId(), evento.valorTotal());

        if (resultado.aprovado()) {
            pagamento.setStatus(StatusPagamento.APROVADO);
        } else {
            pagamento.setStatus(StatusPagamento.RECUSADO);
            pagamento.setMotivo(resultado.motivo());
        }

        pagamentoRepository.save(pagamento);

        // publica resultado para o Hotel Core — spec009
        resultadoProducer.publicar(pagamento);

        log.info("Pagamento processado reservaId={} status={}", evento.reservaId(), pagamento.getStatus());
    }
}
```

---

### ReservaCriadaConsumer.java

```java
@Component
public class ReservaCriadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaCriadaConsumer.class);

    private final PagamentoService pagamentoService;

    public ReservaCriadaConsumer(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @KafkaListener(
        topics = "${kafka.topics.reserva-criada}",
        groupId = "pagamentos-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumir(ReservaCriadaEvent evento) {
        log.info("reserva.criada recebido reservaId={}", evento.reservaId());
        pagamentoService.processar(evento);
    }
}
```

---

## Fluxo de idempotência

```
Kafka entrega reserva.criada (pode ser redelivery)
        │
        ▼
PagamentoService.processar()
        │
        ├── existsByReservaId() → TRUE  → ignora, retorna
        │
        └── existsByReservaId() → FALSE
                │
                ├── Salva Pagamento (PENDENTE)
                ├── Chama gateway
                ├── Atualiza status (APROVADO / RECUSADO)
                └── Publica pagamento.resultado
```

---

**Próxima spec:** [spec009 — Pagamentos Service: Kafka Producer](spec009-pagamentos-producer.md)
