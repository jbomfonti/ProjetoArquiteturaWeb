# SPEC — Model: Reserva

> Pacote: `com.sistema.gestao.sistemagestao.model`
> Depende de: [spec-hospede-model](spec-hospede-model.md), [spec-imovel-model](spec-imovel-model.md)

## StatusReserva.java

```java
package com.sistema.gestao.sistemagestao.model;

public enum StatusReserva {
    PENDENTE,   // aguardando confirmação de pagamento
    CONFIRMADA, // pagamento aprovado
    CANCELADA,  // pagamento recusado ou cancelamento manual
    CONCLUIDA   // hóspede realizou check-out
}
```

---

## Reserva.java

```java
package com.sistema.gestao.sistemagestao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospede_id", nullable = false)
    private Hospede hospede;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @NotNull
    @Column(nullable = false)
    private LocalDate dataCheckIn;

    @NotNull
    @Column(nullable = false)
    private LocalDate dataCheckOut;

    @Min(1)
    @Column(nullable = false)
    private Integer numeroHospedes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.PENDENTE;

    @Version
    private Long version; // lock otimista — previne overbooking em requisições simultâneas

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) this.status = StatusReserva.PENDENTE;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // getters e setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hospede getHospede() { return hospede; }
    public void setHospede(Hospede hospede) { this.hospede = hospede; }

    public Imovel getImovel() { return imovel; }
    public void setImovel(Imovel imovel) { this.imovel = imovel; }

    public LocalDate getDataCheckIn() { return dataCheckIn; }
    public void setDataCheckIn(LocalDate dataCheckIn) { this.dataCheckIn = dataCheckIn; }

    public LocalDate getDataCheckOut() { return dataCheckOut; }
    public void setDataCheckOut(LocalDate dataCheckOut) { this.dataCheckOut = dataCheckOut; }

    public Integer getNumeroHospedes() { return numeroHospedes; }
    public void setNumeroHospedes(Integer numeroHospedes) { this.numeroHospedes = numeroHospedes; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public StatusReserva getStatus() { return status; }
    public void setStatus(StatusReserva status) { this.status = status; }

    public Long getVersion() { return version; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
```

---

## SQL — tabela reservas

```sql
CREATE TABLE reservas (
    id              BIGSERIAL     PRIMARY KEY,
    hospede_id      BIGINT        NOT NULL REFERENCES hospedes(id),
    imovel_id       BIGINT        NOT NULL REFERENCES imoveis(id),
    data_check_in   DATE          NOT NULL,
    data_check_out  DATE          NOT NULL,
    numero_hospedes INT           NOT NULL CHECK (numero_hospedes >= 1),
    valor_total     NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    version         BIGINT        NOT NULL DEFAULT 0,
    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,

    CONSTRAINT chk_datas CHECK (data_check_out > data_check_in)
);

CREATE INDEX idx_reservas_hospede_id ON reservas(hospede_id);
CREATE INDEX idx_reservas_imovel_id  ON reservas(imovel_id);
CREATE INDEX idx_reservas_status     ON reservas(status);
```

---

## Regras de negócio

- `valorTotal` = `precoPorNoite × número de noites`; noites = `dataCheckOut - dataCheckIn` (dias).
- `dataCheckOut` deve ser posterior a `dataCheckIn`.
- Não pode haver duas reservas PENDENTE ou CONFIRMADA para o mesmo imóvel em datas sobrepostas.
- `@Version` garante lock otimista: se duas requisições criarem reserva simultânea, uma delas receberá `OptimisticLockException`.
