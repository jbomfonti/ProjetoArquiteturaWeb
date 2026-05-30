# SPEC — Configuração JPA: Hospede

> Depende de: [spec-hospede-model](spec-hospede-model.md)

## HospedeRepository.java

```java
package com.sistema.gestao.sistemagestao.repository;

import com.sistema.gestao.sistemagestao.model.Hospede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospedeRepository extends JpaRepository<Hospede, Long> {

    Optional<Hospede> findByEmail(String email);

    Optional<Hospede> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}
```

---

## Anotações JPA relevantes na entidade

| Anotação | Campo | Efeito |
|---|---|---|
| `@Entity` | classe | Mapeada para a tabela `hospedes` |
| `@Table(name = "hospedes")` | classe | Nome explícito da tabela |
| `@Id @GeneratedValue(IDENTITY)` | `id` | PK com auto-increment do banco |
| `@Column(unique = true)` | `email`, `cpf` | Unicidade garantida no banco |
| `@Column(nullable = false)` | campos obrigatórios | Constraint NOT NULL no banco |
| `@Column(updatable = false)` | `dataCadastro` | Não pode ser alterado após INSERT |
| `@PrePersist` | `prePersist()` | Preenche `dataCadastro` automaticamente |

---

## Índices criados no banco

```sql
CREATE INDEX idx_hospedes_email ON hospedes(email);
CREATE INDEX idx_hospedes_cpf   ON hospedes(cpf);
```

Esses índices aceleram `findByEmail` e `findByCpf`, que são chamados tanto na criação quanto na validação de duplicatas.

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
      ddl-auto: validate          # nunca altera o schema em produção
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        default_schema: public
  flyway:
    enabled: true
    locations: classpath:db/migration
```

> `ddl-auto: validate` — Hibernate confere se o schema do banco bate com as entidades, mas não cria nem altera tabelas. O schema é gerenciado pelo Flyway.

---

## Comportamento dos métodos herdados de JpaRepository

| Método | Comportamento |
|---|---|
| `save(hospede)` | INSERT se novo, UPDATE se gerenciado |
| `findById(id)` | SELECT por PK, retorna `Optional` |
| `findAll()` | SELECT * hospedes |
| `existsById(id)` | SELECT COUNT — sem carregar a entidade |
| `deleteById(id)` | DELETE por PK, lança `EmptyResultDataAccessException` se não encontrado |
