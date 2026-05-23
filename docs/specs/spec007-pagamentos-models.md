# SPEC-007 — Pagamentos Service: Models e Banco de Dados

---

## Imagens Docker

| Serviço    | Imagem                    | Versão  |
|------------|---------------------------|---------|
| PostgreSQL | `postgres`                | `16`    |
| Kafka      | `confluentinc/cp-kafka`   | `7.6.0` |
| Zookeeper  | `confluentinc/cp-zookeeper` | `7.6.0` |

> Infraestrutura completa em [spec001](spec001-infraestrutura-docker.md).  
> Este serviço usa o banco `pagamentosdb` na porta `5433`.

---

## Objetivo

Criar a entidade `Pagamento`, o enum de status e a migration Flyway do **Pagamentos Service** (microsserviço independente, porta 8081).

---

## Estrutura de arquivos

```
pagamentos-service/src/main/
├── java/com/hotel/pagamentos/
│   ├── model/
│   │   ├── Pagamento.java
│   │   └── StatusPagamento.java
│   └── repository/
│       └── PagamentoRepository.java
└── resources/
    ├── application.yml
    └── db/migration/
        └── V1__create_pagamentos.sql
```

---

## Implementação

### StatusPagamento.java (enum)

```java
public enum StatusPagamento {
    PENDENTE,    // aguardando resposta do gateway
    APROVADO,    // gateway confirmou
    RECUSADO,    // gateway recusou (limite, dados inválidos, etc.)
    ESTORNADO    // pagamento revertido após aprovação
}
```

---

### Pagamento.java

```java
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // referência lógica ao Hotel Core — sem FK cross-service
    @Column(nullable = false, unique = true)
    private Long reservaId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    private String motivo; // preenchido em RECUSADO ou ESTORNADO

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime atualizadoEm;

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // getters e setters
}
```

> **Nota sobre `reservaId`:** este campo é apenas uma referência lógica.
> Não há FK para o banco do Hotel Core — os serviços são desacoplados.
> O vínculo entre domínios acontece via eventos Kafka.

---

### PagamentoRepository.java

```java
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    // usado para idempotência: evita processar a mesma reserva duas vezes
    boolean existsByReservaId(Long reservaId);

    Optional<Pagamento> findByReservaId(Long reservaId);
}
```

---

### V1__create_pagamentos.sql (Flyway migration)

```sql
CREATE TABLE pagamentos (
    id            BIGSERIAL PRIMARY KEY,
    reserva_id    BIGINT         NOT NULL UNIQUE,
    valor         NUMERIC(10,2)  NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    motivo        TEXT,
    criado_em     TIMESTAMP      NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP
);

CREATE INDEX idx_pagamentos_reserva_id ON pagamentos(reserva_id);
```

---

### application.yml (Pagamentos Service)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/pagamentosdb
    username: pagamentos
    password: pagamentos123
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: pagamentos-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.hotel.core.event,com.hotel.pagamentos.event"

server:
  port: 8081

kafka:
  topics:
    reserva-criada: reserva.criada
    pagamento-resultado: pagamento.resultado
```

---

**Próxima spec:** [spec008 — Pagamentos Service: Consumer e Gateway](spec008-pagamentos-consumer-gateway.md)
