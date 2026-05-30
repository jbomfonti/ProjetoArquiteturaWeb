# SPEC — Configuração JPA: Imovel

> Depende de: [spec-imovel-model](spec-imovel-model.md)

## ImovelRepository.java

```java
package com.sistema.gestao.sistemagestao.repository;

import com.sistema.gestao.sistemagestao.model.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ImovelRepository extends JpaRepository<Imovel, Long> {

    List<Imovel> findByDisponivel(Boolean disponivel);

    List<Imovel> findByCapacidadeGreaterThanEqual(Integer capacidadeMinima);

    List<Imovel> findByPrecoPorNoiteLessThanEqual(BigDecimal precoMaximo);

    @Query("""
        SELECT i FROM Imovel i
        WHERE i.disponivel = true
          AND i.capacidade >= :capacidade
          AND i.precoPorNoite <= :precoMaximo
        ORDER BY i.precoPorNoite ASC
    """)
    List<Imovel> buscarDisponiveis(Integer capacidade, BigDecimal precoMaximo);
}
```

---

## Anotações JPA relevantes na entidade

| Anotação | Campo | Efeito |
|---|---|---|
| `@Entity` | classe | Mapeada para a tabela `imoveis` |
| `@Table(name = "imoveis")` | classe | Nome explícito da tabela |
| `@Id @GeneratedValue(IDENTITY)` | `id` | PK com auto-increment do banco |
| `@Column(nullable = false)` | campos obrigatórios | Constraint NOT NULL no banco |
| `@Column(precision = 10, scale = 2)` | `precoPorNoite` | Precisão numérica correta para valores monetários |
| `@Column(updatable = false)` | `criadoEm` | Não pode ser alterado após INSERT |
| `@PrePersist` | `prePersist()` | Preenche `criadoEm` e garante `disponivel = true` |
| `@PreUpdate` | `preUpdate()` | Atualiza `atualizadoEm` em cada UPDATE |

---

## Índice criado no banco

```sql
CREATE INDEX idx_imoveis_disponivel ON imoveis(disponivel);
```

Acelera `findByDisponivel(true)`, que é a query mais frequente (listagem de imóveis disponíveis para reserva).

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
    properties:
      hibernate:
        format_sql: true
        default_schema: public
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## Comportamento dos métodos herdados de JpaRepository

| Método | Comportamento |
|---|---|
| `save(imovel)` | INSERT se novo, UPDATE se gerenciado |
| `findById(id)` | SELECT por PK, retorna `Optional` |
| `findAll()` | SELECT * imoveis |
| `existsById(id)` | SELECT COUNT — sem carregar a entidade |
| `deleteById(id)` | DELETE por PK |

---

## Observação sobre exclusão

Antes de deletar um imóvel, verificar se não há reservas PENDENTE ou CONFIRMADA associadas a ele. Caso existam, a deleção deve ser bloqueada para manter a integridade referencial do banco.
