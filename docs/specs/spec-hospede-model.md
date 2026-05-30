# SPEC — Model: Hospede

> Pacote: `com.sistema.gestao.sistemagestao.model`

## Hospede.java

```java
package com.sistema.gestao.sistemagestao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospedes")
public class Hospede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String telefone;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String cpf;

    @Column
    private LocalDate dataNascimento;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    void prePersist() {
        this.dataCadastro = LocalDateTime.now();
    }

    // getters e setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
}
```

---

## SQL — tabela hospedes

```sql
CREATE TABLE hospedes (
    id             BIGSERIAL    PRIMARY KEY,
    nome           VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    telefone       VARCHAR(20)  NOT NULL,
    cpf            VARCHAR(14)  NOT NULL UNIQUE,
    data_nascimento DATE,
    data_cadastro  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospedes_email ON hospedes(email);
CREATE INDEX idx_hospedes_cpf   ON hospedes(cpf);
```

---

## Regras de negócio

- `cpf` e `email` devem ser únicos — rejeitar cadastro duplicado com erro 409.
- `dataCadastro` é preenchida automaticamente via `@PrePersist`, nunca pelo cliente.
- `dataNascimento` é opcional.
