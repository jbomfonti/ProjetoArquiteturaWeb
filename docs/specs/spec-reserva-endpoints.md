# SPEC — Endpoints CRUD: Reserva

> Depende de: [spec-reserva-model](spec-reserva-model.md), [spec-reserva-jpa](spec-reserva-jpa.md)

## Rotas

| Método | Rota | Ação |
|---|---|---|
| POST | `/api/reservas` | Criar reserva |
| GET | `/api/reservas` | Listar todas / filtrar por `?hospedeId=X` |
| GET | `/api/reservas/{id}` | Buscar reserva por ID |
| PATCH | `/api/reservas/{id}/status` | Atualizar status da reserva |
| DELETE | `/api/reservas/{id}` | Cancelar / remover reserva |

---

## DTOs

### CriarReservaRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CriarReservaRequest(
    @NotNull Long hospedeId,
    @NotNull Long imovelId,
    @NotNull @FutureOrPresent LocalDate dataCheckIn,
    @NotNull LocalDate dataCheckOut,
    @NotNull @Min(1) Integer numeroHospedes
) {}
```

### AtualizarStatusReservaRequest.java

```java
package com.sistema.gestao.sistemagestao.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusReservaRequest(
        @NotNull StatusReserva status
) {
}
```

### ReservaResponse.java

```java
package com.sistema.gestao.sistemagestao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
    Long id,
    Long hospedeId,
    String nomeHospede,
    Long imovelId,
    String nomeImovel,
    LocalDate dataCheckIn,
    LocalDate dataCheckOut,
    Integer numeroHospedes,
    BigDecimal valorTotal,
    String status,
    LocalDateTime criadoEm
) {}
```

---

## ReservaService.java

```java
package com.sistema.gestao.sistemagestao.service;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.model.*;
import com.sistema.gestao.sistemagestao.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HospedeRepository hospedeRepository;
    private final ImovelRepository imovelRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          HospedeRepository hospedeRepository,
                          ImovelRepository imovelRepository) {
        this.reservaRepository = reservaRepository;
        this.hospedeRepository = hospedeRepository;
        this.imovelRepository = imovelRepository;
    }

    public ReservaResponse criar(CriarReservaRequest request) {
        if (!request.dataCheckOut().isAfter(request.dataCheckIn()))
            throw new IllegalArgumentException("Data de check-out deve ser posterior ao check-in");

        Hospede hospede = hospedeRepository.findById(request.hospedeId())
            .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));

        Imovel imovel = imovelRepository.findById(request.imovelId())
            .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));

        if (!imovel.getDisponivel())
            throw new IllegalStateException("Imóvel indisponível");

        if (reservaRepository.existeConflitoDeDatas(
                imovel.getId(), request.dataCheckIn(), request.dataCheckOut()))
            throw new IllegalStateException("Imóvel já reservado para o período solicitado");

        long noites = ChronoUnit.DAYS.between(request.dataCheckIn(), request.dataCheckOut());
        BigDecimal valorTotal = imovel.getPrecoPorNoite().multiply(BigDecimal.valueOf(noites));

        Reserva reserva = new Reserva();
        reserva.setHospede(hospede);
        reserva.setImovel(imovel);
        reserva.setDataCheckIn(request.dataCheckIn());
        reserva.setDataCheckOut(request.dataCheckOut());
        reserva.setNumeroHospedes(request.numeroHospedes());
        reserva.setValorTotal(valorTotal);

        return toResponse(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(Long hospedeId) {
        List<Reserva> reservas = (hospedeId != null)
            ? reservaRepository.findByHospedeId(hospedeId)
            : reservaRepository.findAll();
        return reservas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscarPorId(Long id) {
        return reservaRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
    }

    public ReservaResponse atualizarStatus(Long id, AtualizarStatusReservaRequest request) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
        reserva.setStatus(request.status());
        return toResponse(reservaRepository.save(reserva));
    }

    public void deletar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
        if (reserva.getStatus() == StatusReserva.CONFIRMADA)
            throw new IllegalStateException("Não é possível remover reserva já confirmada — use PATCH /status para cancelar");
        reservaRepository.deleteById(id);
    }

    private ReservaResponse toResponse(Reserva r) {
        return new ReservaResponse(
            r.getId(),
            r.getHospede().getId(), r.getHospede().getNome(),
            r.getImovel().getId(), r.getImovel().getNome(),
            r.getDataCheckIn(), r.getDataCheckOut(),
            r.getNumeroHospedes(), r.getValorTotal(),
            r.getStatus().name(), r.getCriadoEm()
        );
    }
}
```

---

## ReservaController.java

```java
package com.sistema.gestao.sistemagestao.controller;

import com.sistema.gestao.sistemagestao.dto.*;
import com.sistema.gestao.sistemagestao.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Validated
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@RequestBody @Valid CriarReservaRequest request) {
        return reservaService.criar(request);
    }

    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) Long hospedeId) {
        return reservaService.listar(hospedeId);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    public ReservaResponse atualizarStatus(@PathVariable Long id,
                                            @RequestBody @Valid AtualizarStatusReservaRequest request) {
        return reservaService.atualizarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        reservaService.deletar(id);
    }
}
```

---

## Exemplos de uso (curl)

```bash
# Criar reserva
curl -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{"hospedeId":1,"imovelId":1,"dataCheckIn":"2025-08-10","dataCheckOut":"2025-08-15","numeroHospedes":2}'

# Listar todas
curl http://localhost:8080/api/reservas

# Filtrar por hóspede
curl "http://localhost:8080/api/reservas?hospedeId=1"

# Buscar por ID
curl http://localhost:8080/api/reservas/1

# Confirmar reserva
curl -X PATCH http://localhost:8080/api/reservas/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMADA"}'

# Cancelar reserva
curl -X PATCH http://localhost:8080/api/reservas/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CANCELADA"}'

# Remover reserva (apenas PENDENTE ou CANCELADA)
curl -X DELETE http://localhost:8080/api/reservas/1
```

---

## Respostas HTTP

| Situação | Status |
|---|---|
| Reserva criada | 201 Created |
| Reserva encontrada | 200 OK |
| Listagem | 200 OK |
| Status atualizado | 200 OK |
| Reserva removida | 204 No Content |
| ID não encontrado | 404 Not Found |
| Conflito de datas / imóvel indisponível | 409 Conflict |
| Campos inválidos / datas inconsistentes | 400 Bad Request |

---

## Regras de negócio

- `valorTotal` é calculado automaticamente: `precoPorNoite × noites`.
- Não é possível deletar reserva CONFIRMADA — alterar status para CANCELADA primeiro.
- Dois pedidos simultâneos para o mesmo período disparam `OptimisticLockException` (resolvido pelo `@Version` na entidade).
