# SPEC-008 — Pagamentos Service: Consumer e Gateway

> Depende de: [spec007](spec007-pagamentos-models.md) (Pagamento criado)

## O que essa spec faz

Implementa no **pagamentos-service**:
1. `ReservaCriadaConsumer` — consome `reserva.criada` e dispara o processamento
2. `GatewayPagamento` — interface que representa um gateway externo de pagamento
3. `MockGatewayPagamento` — implementação mock (aprova 80% das transações)
4. `PagamentoService` — orquestra tudo com controle de idempotência

---

## Estrutura de arquivos

```
pagamentos-service/src/main/java/com/hotel/pagamentos/
├── kafka/consumer/
│   └── ReservaCriadaConsumer.java
├── gateway/
│   ├── GatewayPagamento.java
│   └── MockGatewayPagamento.java
├── service/
│   └── PagamentoService.java
└── event/
    ├── ReservaCriadaEvent.java      ← cópia local (sem JAR compartilhado)
    └── PagamentoResultadoEvent.java ← cópia local
```

---

## Eventos (cópias locais)

```java
// Consumido de reserva.criada
public record ReservaCriadaEvent(
    Long reservaId, Long hospedeId, Long imovelId,
    LocalDate dataCheckIn, LocalDate dataCheckOut,
    BigDecimal valorTotal
) {}

// Publicado em pagamento.resultado
public record PagamentoResultadoEvent(
    Long reservaId, Long pagamentoId,
    String status,  // APROVADO | RECUSADO | ESTORNADO
    String motivo
) {}
```

> Cada serviço tem sua própria cópia dos eventos — sem dependência de JAR compartilhado.

---

## GatewayPagamento.java (interface)

```java
public interface GatewayPagamento {
    ResultadoGateway processar(Long reservaId, BigDecimal valor);
    record ResultadoGateway(boolean aprovado, String motivo) {}
}
```

---

## MockGatewayPagamento.java

```java
@Component
@Primary
public class MockGatewayPagamento implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(MockGatewayPagamento.class);

    @Override
    public ResultadoGateway processar(Long reservaId, BigDecimal valor) {
        log.info("Gateway mock processando reservaId={} valor={}", reservaId, valor);
        boolean aprovado = Math.random() > 0.2; // 80% de aprovação
        return new ResultadoGateway(aprovado, aprovado ? null : "Limite insuficiente (mock)");
    }
}
```

---

## PagamentoService.java

```java
@Service
@Transactional
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final GatewayPagamento gateway;
    private final PagamentoResultadoProducer resultadoProducer;

    public void processar(ReservaCriadaEvent evento) {
        // se já existe um pagamento para esta reserva, ignora (idempotência)
        if (pagamentoRepository.existsByReservaId(evento.reservaId())) {
            log.warn("Pagamento já processado para reservaId={}, ignorando", evento.reservaId());
            return;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setReservaId(evento.reservaId());
        pagamento.setValor(evento.valorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamentoRepository.save(pagamento);

        GatewayPagamento.ResultadoGateway resultado = gateway.processar(evento.reservaId(), evento.valorTotal());

        pagamento.setStatus(resultado.aprovado() ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO);
        if (!resultado.aprovado()) pagamento.setMotivo(resultado.motivo());
        pagamentoRepository.save(pagamento);

        resultadoProducer.publicar(pagamento); // ver spec009

        log.info("Pagamento processado reservaId={} status={}", evento.reservaId(), pagamento.getStatus());
    }
}
```

---

## ReservaCriadaConsumer.java

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
Kafka entrega reserva.criada (pode ser reentrega)
        │
PagamentoService.processar()
        │
        ├── existsByReservaId() = true  → ignora, retorna
        └── existsByReservaId() = false
                │
                ├── Salva Pagamento (PENDENTE)
                ├── Chama gateway mock
                ├── Atualiza status (APROVADO / RECUSADO)
                └── Publica pagamento.resultado
```

---

**Próxima spec:** [spec009 — Pagamentos Service: Kafka Producer](spec009-pagamentos-producer.md)
