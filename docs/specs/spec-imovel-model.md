# SPEC — Model: Imovel

> Pacote: `com.sistema.gestao.sistemagestao.model`

## Imovel.java

```java
package com.sistema.gestao.sistemagestao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "imoveis")
public class Imovel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacidade;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoPorNoite;

    @Column(nullable = false)
    private Boolean disponivel = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.disponivel = true;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // getters e setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

    public BigDecimal getPrecoPorNoite() { return precoPorNoite; }
    public void setPrecoPorNoite(BigDecimal precoPorNoite) { this.precoPorNoite = precoPorNoite; }

    public Boolean getDisponivel() { return disponivel; }
    public void setDisponivel(Boolean disponivel) { this.disponivel = disponivel; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
```

---

## SQL — tabela imoveis

```sql
CREATE TABLE imoveis (
    id              BIGSERIAL     PRIMARY KEY,
    nome            VARCHAR(255)  NOT NULL,
    descricao       TEXT          NOT NULL,
    capacidade      INT           NOT NULL CHECK (capacidade >= 1),
    preco_por_noite NUMERIC(10,2) NOT NULL CHECK (preco_por_noite > 0),
    disponivel      BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP
);

CREATE INDEX idx_imoveis_disponivel ON imoveis(disponivel);
```

---

## Regras de negócio

- `disponivel = true` ao criar — imóvel começa disponível por padrão.
- Imóvel com `disponivel = false` não pode receber novas reservas.
- `precoPorNoite` deve ser maior que zero.
- `capacidade` deve ser pelo menos 1.
