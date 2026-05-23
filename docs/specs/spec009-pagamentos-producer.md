# SPEC-009 — Pagamentos Service: Kafka Producer

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

Implementar o **producer Kafka** do Pagamentos Service que publica o resultado do pagamento no tópico `pagamento.resultado` após processar a transação com o gateway.

---

## Estrutura de arquivos

```
pagamentos-service/src/main/java/com/hotel/pagamentos/
├── kafka/
│   └── producer/
│       └── PagamentoResultadoProducer.java
└── event/
    └── PagamentoResultadoEvent.java
```

---

## Implementação

### PagamentoResultadoEvent.java (cópia local)

```java
// O Pagamentos Service mantém sua própria cópia — sem JAR compartilhado
public record PagamentoResultadoEvent(
    Long reservaId,
    Long pagamentoId,
    String status,   // APROVADO | RECUSADO | ESTORNADO
    String motivo
) {}
```

---

### PagamentoResultadoProducer.java

```java
@Component
public class PagamentoResultadoProducer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoResultadoProducer.class);

    @Value("${kafka.topics.pagamento-resultado}")
    private String topico;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagamentoResultadoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(Pagamento pagamento) {
        PagamentoResultadoEvent evento = new PagamentoResultadoEvent(
            pagamento.getReservaId(),
            pagamento.getId(),
            pagamento.getStatus().name(),
            pagamento.getMotivo()
        );

        // mesma chave do producer do Hotel Core — garante ordem por reserva
        String chave = pagamento.getReservaId().toString();

        kafkaTemplate.send(topico, chave, evento)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Falha ao publicar pagamento.resultado reservaId={}", pagamento.getReservaId(), ex);
                } else {
                    log.info("pagamento.resultado publicado reservaId={} status={} partition={} offset={}",
                        pagamento.getReservaId(),
                        pagamento.getStatus(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                    );
                }
            });
    }
}
```

---

### KafkaConfig.java (Pagamentos Service)

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, ReservaCriadaEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pagamentos-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.hotel.core.event,com.hotel.pagamentos.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ReservaCriadaEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservaCriadaEvent> kafkaListenerContainerFactory(
        ConsumerFactory<String, ReservaCriadaEvent> consumerFactory,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReservaCriadaEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(2000L, 3L)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public NewTopic topicPagamentoResultado() {
        return TopicBuilder.name("pagamento.resultado").partitions(3).replicas(1).build();
    }
}
```

---

## Visão geral do fluxo completo

```
Hotel Core (8080)                  Kafka                   Pagamentos Service (8081)
─────────────────                  ─────                   ─────────────────────────
POST /api/reservas
        │
   Salva PENDENTE
        │
   ReservaProducer ──► reserva.criada ──────────────────► ReservaCriadaConsumer
                                                                    │
                                                           PagamentoService.processar()
                                                                    │
                                                           GatewayPagamento (mock)
                                                                    │
                                                        PagamentoResultadoProducer
                                                                    │
PagamentoResultadoConsumer ◄── pagamento.resultado ◄────────────────┘
        │
Atualiza Reserva
→ CONFIRMADA / CANCELADA
```

---

**Próxima spec:** [spec010 — Testes (após endpoints)](spec010-testes.md)
