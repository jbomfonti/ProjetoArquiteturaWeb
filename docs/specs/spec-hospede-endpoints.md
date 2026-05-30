# SPEC — Endpoints CRUD: Hospede

> Depende de: [spec-hospede-model](spec-hospede-model.md), [spec-hospede-jpa](spec-hospede-jpa.md)

## Rotas

| Método | Rota | Ação |
|---|---|---|
| POST | `/api/hospedes` | Cadastrar hóspede |
| GET | `/api/hospedes` | Listar todos os hóspedes |
| GET | `/api/hospedes/{id}` | Buscar hóspede por ID |
| PUT | `/api/hospedes/{id}` | Atualizar hóspede |
| DELETE | `/api/hospedes/{id}` | Remover hóspede |

---

## DTOs

### CriarHospedeRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CriarHospedeRequest(
    @NotBlank String nome,
    @NotBlank @Email String email,
    @NotBlank String telefone,
    @NotBlank String cpf,
    LocalDate dataNascimento
) {}
```

### AtualizarHospedeRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AtualizarHospedeRequest(
    @NotBlank String nome,
    @NotBlank @Email String email,
    @NotBlank String telefone,
    LocalDate dataNascimento
) {}
```

### HospedeResponse.java

```java
package com.sistema.gestao.sistemagestao.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HospedeResponse(
    Long id,
    String nome,
    String email,
    String telefone,
    String cpf,
    LocalDate dataNascimento,
    LocalDateTime dataCadastro
) {}
```

---

## HospedeService.java

```java
package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import com.sistema.gestao.sistemagestao.model.Hospede;
import com.sistema.gestao.sistemagestao.repository.HospedeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HospedeService {

    private final HospedeRepository hospedeRepository;

    public HospedeService(HospedeRepository hospedeRepository) {
        this.hospedeRepository = hospedeRepository;
    }

    public HospedeResponse criar(CriarHospedeRequest request) {
        if (hospedeRepository.existsByEmail(request.email()))
            throw new IllegalStateException("E-mail já cadastrado");
        if (hospedeRepository.existsByCpf(request.cpf()))
            throw new IllegalStateException("CPF já cadastrado");

        Hospede hospede = new Hospede();
        hospede.setNome(request.nome());
        hospede.setEmail(request.email());
        hospede.setTelefone(request.telefone());
        hospede.setCpf(request.cpf());
        hospede.setDataNascimento(request.dataNascimento());
        return toResponse(hospedeRepository.save(hospede));
    }

    @Transactional(readOnly = true)
    public List<HospedeResponse> listar() {
        return hospedeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HospedeResponse buscarPorId(Long id) {
        return hospedeRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));
    }

    public HospedeResponse atualizar(Long id, AtualizarHospedeRequest request) {
        Hospede hospede = hospedeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));

        if (!hospede.getEmail().equals(request.email()) && hospedeRepository.existsByEmail(request.email()))
            throw new IllegalStateException("E-mail já cadastrado por outro hóspede");

        hospede.setNome(request.nome());
        hospede.setEmail(request.email());
        hospede.setTelefone(request.telefone());
        hospede.setDataNascimento(request.dataNascimento());
        return toResponse(hospedeRepository.save(hospede));
    }

    public void deletar(Long id) {
        if (!hospedeRepository.existsById(id))
            throw new EntityNotFoundException("Hóspede não encontrado");
        hospedeRepository.deleteById(id);
    }

    private HospedeResponse toResponse(Hospede h) {
        return new HospedeResponse(
            h.getId(), h.getNome(), h.getEmail(), h.getTelefone(),
            h.getCpf(), h.getDataNascimento(), h.getDataCadastro()
        );
    }
}
```

---

## HospedeController.java

```java
package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.AtualizarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.CriarHospedeRequest;
import com.sistema.gestao.sistemagestao.dto.HospedeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospedes")
@Validated
public class HospedeController {

    private final HospedeService hospedeService;

    public HospedeController(HospedeService hospedeService) {
        this.hospedeService = hospedeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospedeResponse criar(@RequestBody @Valid CriarHospedeRequest request) {
        return hospedeService.criar(request);
    }

    @GetMapping
    public List<HospedeResponse> listar() {
        return hospedeService.listar();
    }

    @GetMapping("/{id}")
    public HospedeResponse buscar(@PathVariable Long id) {
        return hospedeService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public HospedeResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid AtualizarHospedeRequest request) {
        return hospedeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        hospedeService.deletar(id);
    }
}
```

---

## Exemplos de uso (curl)

```bash
# Criar hóspede
curl -X POST http://localhost:8080/api/hospedes \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","email":"joao@email.com","telefone":"11999999999","cpf":"123.456.789-00","dataNascimento":"1990-05-15"}'

# Listar todos
curl http://localhost:8080/api/hospedes

# Buscar por ID
curl http://localhost:8080/api/hospedes/1

# Atualizar
curl -X PUT http://localhost:8080/api/hospedes/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Santos","email":"joao@email.com","telefone":"11988888888"}'

# Deletar
curl -X DELETE http://localhost:8080/api/hospedes/1
```

---

## Respostas HTTP

| Situação | Status |
|---|---|
| Hóspede criado | 201 Created |
| Hóspede encontrado | 200 OK |
| Listagem | 200 OK |
| Hóspede atualizado | 200 OK |
| Hóspede deletado | 204 No Content |
| ID não encontrado | 404 Not Found |
| E-mail ou CPF duplicado | 409 Conflict |
| Campos inválidos | 400 Bad Request |
