# SPEC — Configuração JPA: Reserva

> Depende de: [spec-reserva-model](spec-reserva-model.md), [spec-hospede-jpa](spec-hospede-jpa.md), [spec-imovel-jpa](spec-imovel-jpa.md)

## ReservaRepository.java

```java
package com.sistema.gestao.sistemagestao.repository;

import com.sistema.gestao.sistemagestao.model.Reserva;
import com.sistema.gestao.sistemagestao.model.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByHospedeId(Long hospedeId);

    List<Reserva> findByImovelId(Long imovelId);

    List<Reserva> findByStatus(StatusReserva status);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.imovel.id = :imovelId
          AND r.status IN (com.sistema.gestao.sistemagestao.model.StatusReserva.PENDENTE,
                           com.sistema.gestao.sistemagestao.model.StatusReserva.CONFIRMADA)
          AND r.dataCheckIn  < :checkOut
          AND r.dataCheckOut > :checkIn
    """)
    boolean existeConflitoDeDatas(Long imovelId, LocalDate checkIn, LocalDate checkOut);

    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.hospede
        JOIN FETCH r.imovel
        WHERE r.id = :id
    """)
    java.util.Optional<Reserva> findByIdComDetalhes(Long id);
}
```

---

## Anotações JPA relevantes na entidade

| Anotação | Campo | Efeito |
|---|---|---|
| `@Entity` | classe | Mapeada para a tabela `reservas` |
| `@Table(name = "reservas")` | classe | Nome explícito da tabela |
| `@Id @GeneratedValue(IDENTITY)` | `id` | PK com auto-increment |
| `@ManyToOne(fetch = LAZY)` | `hospede`, `imovel` | Evita N+1: carrega sob demanda |
| `@JoinColumn(nullable = false)` | `hospede_id`, `imovel_id` | FK NOT NULL no banco |
| `@Enumerated(STRING)` | `status` | Salva o nome do enum (ex: "PENDENTE") em vez do ordinal |
| `@Version` | `version` | Lock otimista — evita overbooking em requisições simultâneas |
| `@Column(updatable = false)` | `criadoEm` | Não pode ser alterado após INSERT |
| `@PrePersist` | `prePersist()` | Preenche `criadoEm` e garante `status = PENDENTE` |
| `@PreUpdate` | `preUpdate()` | Atualiza `atualizadoEm` em cada UPDATE |

---

## Índices criados no banco

```sql
CREATE INDEX idx_reservas_hospede_id ON reservas(hospede_id);
CREATE INDEX idx_reservas_imovel_id  ON reservas(imovel_id);
CREATE INDEX idx_reservas_status     ON reservas(status);
```

- `hospede_id` — acelera `findByHospedeId`, chamado frequentemente para listar reservas do hóspede.
- `imovel_id` + `status` — acelera `existeConflitoDeDatas`, a query de verificação de conflito antes de cada nova reserva.

---

## Lock otimista com @Version

O campo `version` é incrementado pelo Hibernate a cada UPDATE. Se duas transações simultâneas lerem a mesma reserva e tentarem atualizá-la, a segunda lançará `OptimisticLockException`, que deve ser traduzida para HTTP 409 no `GlobalExceptionHandler`:

```java
@ExceptionHandler(OptimisticLockingFailureException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public Map<String, String> handleOptimisticLock(OptimisticLockingFailureException ex) {
    return Map.of("erro", "Conflito de atualização simultânea — tente novamente");
}
```

---

## Fetch LAZY e problema N+1

`hospede` e `imovel` são carregados com `FetchType.LAZY`. Para evitar N+1 ao serializar a lista de reservas, use `findByIdComDetalhes` (com JOIN FETCH) ou configure um `@EntityGraph`:

```java
@EntityGraph(attributePaths = {"hospede", "imovel"})
List<Reserva> findByHospedeId(Long hospedeId);
```

---

## Configuração Spring Data JPA (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/pgarquiteturaweb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false          # evita lazy loading fora da transação
    properties:
      hibernate:
        format_sql: true
        default_schema: public
  flyway:
    enabled: true
    locations: classpath:db/migration
```

> `open-in-view: false` — desliga o padrão anti-pattern OSIV. Garante que qualquer acesso lazy fora da transação lance `LazyInitializationException`, tornando o problema visível em vez de silencioso.
