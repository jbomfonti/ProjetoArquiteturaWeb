# SPEC-004 — Hotel Core: Kafka Config e Eventos

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

Configurar o Kafka no **Hotel Core**:

- `KafkaConfig.java` — producer, consumer factory, retry e DLT
- `ReservaCriadaEvent.java` — payload publicado no tópico `reserva.criada`
- `PagamentoResultadoEvent.java` — payload consumido do tópico `pagamento.resultado`

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
├── config/
│   └── KafkaConfig.java
└── event/
    ├── ReservaCriadaEvent.java
    └── PagamentoResultadoEvent.java
```

---

## Implementação

### application.yml — seção Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: hotel-core-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.hotel.core.event,com.hotel.pagamentos.event"

kafka:
  topics:
    reserva-criada: reserva.criada
    pagamento-resultado: pagamento.resultado
```

---

### ReservaCriadaEvent.java

```java
// Publicado pelo Hotel Core → consumido pelo Pagamentos Service
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

### PagamentoResultadoEvent.java

```java
// Publicado pelo Pagamentos Service → consumido pelo Hotel Core
public record PagamentoResultadoEvent(
    Long reservaId,
    Long pagamentoId,
    String status,   // APROVADO | RECUSADO | ESTORNADO
    String motivo    // preenchido quando RECUSADO ou ESTORNADO
) {}
```

---

### KafkaConfig.java

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer ──────────────────────────────────────────────────────────
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

    // ── Consumer ──────────────────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, PagamentoResultadoEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hotel-core-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.hotel.core.event,com.hotel.pagamentos.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PagamentoResultadoEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // ── Retry + Dead Letter Topic ─────────────────────────────────────────
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PagamentoResultadoEvent> kafkaListenerContainerFactory(
        ConsumerFactory<String, PagamentoResultadoEvent> consumerFactory,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, PagamentoResultadoEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 3 tentativas com intervalo de 2s entre elas
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(2000L, 3L)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ── Tópicos (criação automática) ─────────────────────────────────────
    @Bean
    public NewTopic topicReservaCriada() {
        return TopicBuilder.name("reserva.criada").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicPagamentoResultado() {
        return TopicBuilder.name("pagamento.resultado").partitions(3).replicas(1).build();
    }
}
```

---

## Transição de status via evento

```
pagamento.resultado.status  →  Reserva.status
──────────────────────────────────────────────
APROVADO                    →  CONFIRMADA
RECUSADO                    →  CANCELADA
ESTORNADO                   →  CANCELADA
```

---

**Próxima spec:** [spec005 — Hotel Core: Kafka Producer](spec005-hotelcore-producer.md)
