# SPEC-007 — Pagamentos Service: Models e Banco

> Depende de: [spec001](spec001-infraestrutura-docker.md) (infraestrutura rodando)
> Este serviço usa o banco `pagamentosdb` na porta `5433` e roda na porta `8081`.

## O que essa spec faz

Cria a entidade `Pagamento`, o enum de status e a migration Flyway do **pagamentos-service**.

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

## StatusPagamento.java (enum)

```java
public enum StatusPagamento {
    PENDENTE,   // aguardando resposta do gateway
    APROVADO,   // gateway confirmou
    RECUSADO,   // gateway recusou
    ESTORNADO   // pagamento revertido após aprovação
}
```

---

## Pagamento.java

```java
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long reservaId; // referência lógica — sem FK cross-service

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
    void preUpdate() { this.atualizadoEm = LocalDateTime.now(); }

    // getters e setters
}
```

> `reservaId` é só uma referência lógica — não existe FK apontando para o banco do hotel-core. O vínculo entre os serviços acontece via eventos Kafka.

---

## PagamentoRepository.java

```java
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    boolean existsByReservaId(Long reservaId); // usado para idempotência

    Optional<Pagamento> findByReservaId(Long reservaId);
}
```

---

## V1__create_pagamentos.sql

```sql
CREATE TABLE pagamentos (
    id            BIGSERIAL PRIMARY KEY,
    reserva_id    BIGINT        NOT NULL UNIQUE,
    valor         NUMERIC(10,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    motivo        TEXT,
    criado_em     TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP
);

CREATE INDEX idx_pagamentos_reserva_id ON pagamentos(reserva_id);
```

---

## application.yml (Pagamentos Service)

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
