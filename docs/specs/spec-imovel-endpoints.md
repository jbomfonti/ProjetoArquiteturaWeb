# SPEC — Endpoints CRUD: Imovel

> Depende de: [spec-imovel-model](spec-imovel-model.md), [spec-imovel-jpa](spec-imovel-jpa.md)

## Rotas

| Método | Rota | Ação |
|---|---|---|
| POST | `/api/imoveis` | Cadastrar imóvel |
| GET | `/api/imoveis` | Listar imóveis (filtro opcional: `?disponivel=true`) |
| GET | `/api/imoveis/{id}` | Buscar imóvel por ID |
| PUT | `/api/imoveis/{id}` | Atualizar imóvel |
| PATCH | `/api/imoveis/{id}/disponibilidade` | Alterar disponibilidade |
| DELETE | `/api/imoveis/{id}` | Remover imóvel |

---

## DTOs

### CriarImovelRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CriarImovelRequest(
    @NotBlank String nome,
    @NotBlank String descricao,
    @NotNull @Min(1) Integer capacidade,
    @NotNull @DecimalMin("0.01") BigDecimal precoPorNoite
) {}
```

### AtualizarImovelRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AtualizarImovelRequest(
    @NotBlank String nome,
    @NotBlank String descricao,
    @NotNull @Min(1) Integer capacidade,
    @NotNull @DecimalMin("0.01") BigDecimal precoPorNoite
) {}
```

### AlterarDisponibilidadeRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarDisponibilidadeRequest(
    @NotNull Boolean disponivel
) {}
```

### ImovelResponse.java

```java
package com.sistema.gestao.sistemagestao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImovelResponse(
    Long id,
    String nome,
    String descricao,
    Integer capacidade,
    BigDecimal precoPorNoite,
    Boolean disponivel,
    LocalDateTime criadoEm
) {}
```

---

## ImovelService.java

```java
package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.Imovel;
import com.sistema.gestao.sistemagestao.repository.ImovelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ImovelService {

    private final ImovelRepository imovelRepository;

    public ImovelService(ImovelRepository imovelRepository) {
        this.imovelRepository = imovelRepository;
    }

    public ImovelResponse criar(CriarImovelRequest request) {
        Imovel imovel = new Imovel();
        imovel.setNome(request.nome());
        imovel.setDescricao(request.descricao());
        imovel.setCapacidade(request.capacidade());
        imovel.setPrecoPorNoite(request.precoPorNoite());
        return toResponse(imovelRepository.save(imovel));
    }

    @Transactional(readOnly = true)
    public List<ImovelResponse> listar(Boolean disponivel) {
        List<Imovel> imoveis = (disponivel != null)
            ? imovelRepository.findByDisponivel(disponivel)
            : imovelRepository.findAll();
        return imoveis.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ImovelResponse buscarPorId(Long id) {
        return imovelRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));
    }

    public ImovelResponse atualizar(Long id, AtualizarImovelRequest request) {
        Imovel imovel = imovelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));

        imovel.setNome(request.nome());
        imovel.setDescricao(request.descricao());
        imovel.setCapacidade(request.capacidade());
        imovel.setPrecoPorNoite(request.precoPorNoite());
        return toResponse(imovelRepository.save(imovel));
    }

    public ImovelResponse alterarDisponibilidade(Long id, AlterarDisponibilidadeRequest request) {
        Imovel imovel = imovelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));
        imovel.setDisponivel(request.disponivel());
        return toResponse(imovelRepository.save(imovel));
    }

    public void deletar(Long id) {
        if (!imovelRepository.existsById(id))
            throw new EntityNotFoundException("Imóvel não encontrado");
        imovelRepository.deleteById(id);
    }

    private ImovelResponse toResponse(Imovel i) {
        return new ImovelResponse(
            i.getId(), i.getNome(), i.getDescricao(),
            i.getCapacidade(), i.getPrecoPorNoite(),
            i.getDisponivel(), i.getCriadoEm()
        );
    }
}
```

---

## ImovelController.java

```java
package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ImovelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imoveis")
@Validated
public class ImovelController {

    private final ImovelService imovelService;

    public ImovelController(ImovelService imovelService) {
        this.imovelService = imovelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImovelResponse criar(@RequestBody @Valid CriarImovelRequest request) {
        return imovelService.criar(request);
    }

    @GetMapping
    public List<ImovelResponse> listar(@RequestParam(required = false) Boolean disponivel) {
        return imovelService.listar(disponivel);
    }

    @GetMapping("/{id}")
    public ImovelResponse buscar(@PathVariable Long id) {
        return imovelService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ImovelResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid AtualizarImovelRequest request) {
        return imovelService.atualizar(id, request);
    }

    @PatchMapping("/{id}/disponibilidade")
    public ImovelResponse alterarDisponibilidade(@PathVariable Long id,
                                                  @RequestBody @Valid AlterarDisponibilidadeRequest request) {
        return imovelService.alterarDisponibilidade(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        imovelService.deletar(id);
    }
}
```

---

## Exemplos de uso (curl)

```bash
# Criar imóvel
curl -X POST http://localhost:8080/api/imoveis \
  -H "Content-Type: application/json" \
  -d '{"nome":"Chalé da Serra","descricao":"Vista para as montanhas","capacidade":4,"precoPorNoite":350.00}'

# Listar todos
curl http://localhost:8080/api/imoveis

# Listar apenas disponíveis
curl "http://localhost:8080/api/imoveis?disponivel=true"

# Buscar por ID
curl http://localhost:8080/api/imoveis/1

# Atualizar
curl -X PUT http://localhost:8080/api/imoveis/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"Chalé da Serra Premium","descricao":"Vista para as montanhas","capacidade":6,"precoPorNoite":420.00}'

# Bloquear imóvel
curl -X PATCH http://localhost:8080/api/imoveis/1/disponibilidade \
  -H "Content-Type: application/json" \
  -d '{"disponivel":false}'

# Deletar
curl -X DELETE http://localhost:8080/api/imoveis/1
```

---

## Respostas HTTP

| Situação | Status |
|---|---|
| Imóvel criado | 201 Created |
| Imóvel encontrado | 200 OK |
| Listagem | 200 OK |
| Imóvel atualizado | 200 OK |
| Disponibilidade alterada | 200 OK |
| Imóvel deletado | 204 No Content |
| ID não encontrado | 404 Not Found |
| Campos inválidos | 400 Bad Request |
