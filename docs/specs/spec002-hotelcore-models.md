# SPEC-002 — Hotel Core: Models e Banco de Dados

> Depende de: [spec001](spec001-infraestrutura-docker.md) (infraestrutura rodando)

## O que essa spec faz

Cria as 3 entidades JPA e a migration SQL do **hotel-core**.

---

## Estrutura de arquivos

```
hotel-core/src/main/
├── java/com/hotel/core/model/
│   ├── Hospede.java
│   ├── Imovel.java
│   └── Reserva.java
└── resources/db/migration/
    └── V1__create_tables.sql
```

---

## Hospede.java

```java
@Entity
@Table(name = "hospedes")
public class Hospede {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false, unique = true)
    private String cpf;

    // getters e setters
}
```

---

## Imovel.java

```java
@Entity
@Table(name = "imoveis")
public class Imovel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private Integer capacidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoPorNoite;

    @Column(nullable = false)
    private Boolean disponivel = true;

    // getters e setters
}
```

---

## Reserva.java

```java
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospede_id", nullable = false)
    private Hospede hospede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @Column(nullable = false)
    private LocalDate dataCheckIn;

    @Column(nullable = false)
    private LocalDate dataCheckOut;

    @Column(nullable = false)
    private Integer numeroHospedes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.PENDENTE;

    @Version
    private Long version; // previne overbooking em requisições simultâneas

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime atualizadoEm;

    @PreUpdate
    void preUpdate() { this.atualizadoEm = LocalDateTime.now(); }

    // getters e setters
}
```

---

## StatusReserva.java (enum)

```java
public enum StatusReserva {
    PENDENTE,   // aguardando pagamento
    CONFIRMADA, // pagamento aprovado
    CANCELADA,  // pagamento recusado ou cancelamento manual
    CONCLUIDA   // hóspede fez check-out
}
```

---

## V1__create_tables.sql

```sql
CREATE TABLE hospedes (
    id       BIGSERIAL PRIMARY KEY,
    nome     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    telefone VARCHAR(20)  NOT NULL,
    cpf      VARCHAR(14)  NOT NULL UNIQUE
);

CREATE TABLE imoveis (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255)  NOT NULL,
    descricao       TEXT          NOT NULL,
    capacidade      INT           NOT NULL,
    preco_por_noite NUMERIC(10,2) NOT NULL,
    disponivel      BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE reservas (
    id              BIGSERIAL PRIMARY KEY,
    hospede_id      BIGINT        NOT NULL REFERENCES hospedes(id),
    imovel_id       BIGINT        NOT NULL REFERENCES imoveis(id),
    data_check_in   DATE          NOT NULL,
    data_check_out  DATE          NOT NULL,
    numero_hospedes INT           NOT NULL,
    valor_total     NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    version         BIGINT        NOT NULL DEFAULT 0,
    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP
);
```

---

## application.yml (Hotel Core)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hoteldb
    username: hotel
    password: hotel123
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
```

---

## Regras de negócio

- `valorTotal` = `precoPorNoite × número de noites`
- Número de noites = `dataCheckOut - dataCheckIn` (em dias)

---

**Próxima spec:** [spec003 — Hotel Core: REST Endpoints](spec003-hotelcore-endpoints.md)
