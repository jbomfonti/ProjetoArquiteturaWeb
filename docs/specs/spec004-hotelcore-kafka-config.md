# SPEC-004 — Hotel Core: Kafka Config e Eventos

> Depende de: [spec001](spec001-infraestrutura-docker.md) (Kafka rodando)

## O que essa spec faz

Configura o Kafka no **hotel-core**: producer, consumer, retry com DLT, e os dois eventos trafegados.

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
├── config/
│   └── KafkaConfig.java
└── event/
    ├── ReservaCriadaEvent.java      ← hotel-core publica, pagamentos-service consome
    └── PagamentoResultadoEvent.java ← pagamentos-service publica, hotel-core consome
```

---

## Eventos

### ReservaCriadaEvent.java

```java
public record ReservaCriadaEvent(
    Long reservaId,
    Long hospedeId,
    Long imovelId,
    LocalDate dataCheckIn,
    LocalDate dataCheckOut,
    BigDecimal valorTotal
) {}
```

### PagamentoResultadoEvent.java

```java
public record PagamentoResultadoEvent(
    Long reservaId,
    Long pagamentoId,
    String status,  // APROVADO | RECUSADO | ESTORNADO
    String motivo   // preenchido quando RECUSADO ou ESTORNADO
) {}
```

---

## application.yml — seção Kafka

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

## KafkaConfig.java

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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PagamentoResultadoEvent> kafkaListenerContainerFactory(
        ConsumerFactory<String, PagamentoResultadoEvent> consumerFactory,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PagamentoResultadoEvent>();
        factory.setConsumerFactory(consumerFactory);
        // 3 tentativas com 2s de intervalo; depois manda para o DLT
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(2000L, 3L)
        ));
        return factory;
    }

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

## Como o status da reserva muda com base no evento

| `PagamentoResultadoEvent.status` | Novo `Reserva.status` |
|---|---|
| `APROVADO` | `CONFIRMADA` |
| `RECUSADO` | `CANCELADA` |
| `ESTORNADO` | `CANCELADA` |

---

**Próxima spec:** [spec005 — Hotel Core: Kafka Producer](spec005-hotelcore-producer.md)
