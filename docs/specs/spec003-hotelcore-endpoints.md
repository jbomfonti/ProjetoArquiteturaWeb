# SPEC-003 — Hotel Core: REST Endpoints

---

## Imagens Docker

| Serviço    | Imagem           | Versão  |
|------------|------------------|---------|
| PostgreSQL | `postgres`       | `16`    |
| Kafka      | `confluentinc/cp-kafka` | `7.6.0` |

> Infraestrutura completa em [spec001](spec001-infraestrutura-docker.md).

---

## Objetivo

Implementar os endpoints REST do **Hotel Core**:

| Método | Rota                      | Descrição                        |
|--------|---------------------------|----------------------------------|
| POST   | `/api/hospedes`           | Cadastrar hóspede                |
| GET    | `/api/hospedes/{id}`      | Buscar hóspede por ID            |
| POST   | `/api/reservas`           | Criar reserva (dispara Kafka)    |
| GET    | `/api/reservas/{id}`      | Buscar reserva por ID            |
| GET    | `/api/reservas`           | Listar reservas por hóspede      |

---

## Estrutura de arquivos

```
hotel-core/src/main/java/com/hotel/core/
├── controller/
│   ├── HospedeController.java
│   └── ReservaController.java
├── service/
│   └── ReservaService.java
├── repository/
│   ├── HospedeRepository.java
│   ├── ImovelRepository.java
│   └── ReservaRepository.java
├── dto/
│   ├── CriarHospedeRequest.java
│   ├── CriarReservaRequest.java
│   └── ReservaResponse.java
└── exception/
    └── GlobalExceptionHandler.java
```

---

## Implementação

### CriarReservaRequest.java (DTO)

```java
public record CriarReservaRequest(
    @NotNull Long hospedeId,
    @NotNull Long imovelId,
    @NotNull @FutureOrPresent LocalDate dataCheckIn,
    @NotNull @Future LocalDate dataCheckOut,
    @Min(1) Integer numeroHospedes
) {}
```

---

### ReservaResponse.java (DTO)

```java
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

### ReservaRepository.java

```java
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByHospedeId(Long hospedeId);

    // verifica conflito de datas para o mesmo imóvel
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.imovel.id = :imovelId
          AND r.status IN ('PENDENTE', 'CONFIRMADA')
          AND r.dataCheckIn < :checkOut
          AND r.dataCheckOut > :checkIn
    """)
    boolean existeConflitoDeDatas(Long imovelId, LocalDate checkIn, LocalDate checkOut);
}
```

---

### ReservaService.java

```java
@Service
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HospedeRepository hospedeRepository;
    private final ImovelRepository imovelRepository;
    private final ReservaProducer reservaProducer;

    public ReservaResponse criarReserva(CriarReservaRequest request) {
        Hospede hospede = hospedeRepository.findById(request.hospedeId())
            .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));

        Imovel imovel = imovelRepository.findById(request.imovelId())
            .orElseThrow(() -> new EntityNotFoundException("Imóvel não encontrado"));

        if (!imovel.getDisponivel()) {
            throw new IllegalStateException("Imóvel indisponível");
        }

        boolean conflito = reservaRepository.existeConflitoDeDatas(
            imovel.getId(), request.dataCheckIn(), request.dataCheckOut()
        );
        if (conflito) {
            throw new IllegalStateException("Imóvel já reservado para o período solicitado");
        }

        long noites = ChronoUnit.DAYS.between(request.dataCheckIn(), request.dataCheckOut());
        BigDecimal valorTotal = imovel.getPrecoPorNoite().multiply(BigDecimal.valueOf(noites));

        Reserva reserva = new Reserva();
        reserva.setHospede(hospede);
        reserva.setImovel(imovel);
        reserva.setDataCheckIn(request.dataCheckIn());
        reserva.setDataCheckOut(request.dataCheckOut());
        reserva.setNumeroHospedes(request.numeroHospedes());
        reserva.setValorTotal(valorTotal);
        reserva.setStatus(StatusReserva.PENDENTE);

        reservaRepository.save(reserva);

        // publica evento no Kafka — spec005
        reservaProducer.publicar(reserva);

        return toResponse(reserva);
    }

    public ReservaResponse buscarPorId(Long id) {
        return reservaRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));
    }

    private ReservaResponse toResponse(Reserva r) {
        return new ReservaResponse(
            r.getId(), r.getHospede().getId(), r.getHospede().getNome(),
            r.getImovel().getId(), r.getImovel().getNome(),
            r.getDataCheckIn(), r.getDataCheckOut(),
            r.getNumeroHospedes(), r.getValorTotal(),
            r.getStatus().name(), r.getCriadoEm()
        );
    }
}
```

---

### ReservaController.java

```java
@RestController
@RequestMapping("/api/reservas")
@Validated
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@RequestBody @Valid CriarReservaRequest request) {
        return reservaService.criarReserva(request);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @GetMapping
    public List<ReservaResponse> listarPorHospede(@RequestParam Long hospedeId) {
        return reservaService.listarPorHospede(hospedeId);
    }
}
```

---

### HospedeController.java

```java
@RestController
@RequestMapping("/api/hospedes")
@Validated
public class HospedeController {

    private final HospedeRepository hospedeRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Hospede criar(@RequestBody @Valid CriarHospedeRequest request) {
        Hospede hospede = new Hospede();
        hospede.setNome(request.nome());
        hospede.setEmail(request.email());
        hospede.setTelefone(request.telefone());
        hospede.setCpf(request.cpf());
        return hospedeRepository.save(hospede);
    }

    @GetMapping("/{id}")
    public Hospede buscar(@PathVariable Long id) {
        return hospedeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Hóspede não encontrado"));
    }
}
```

---

### GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(EntityNotFoundException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(IllegalStateException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
        return Map.of("erros", erros);
    }
}
```

---

## Exemplo de requisição

```bash
# Criar hóspede
curl -X POST http://localhost:8080/api/hospedes \
  -H "Content-Type: application/json" \
  -d '{"nome": "João Silva", "email": "joao@email.com", "telefone": "11999999999", "cpf": "123.456.789-00"}'

# Criar reserva (dispara fluxo Kafka completo)
curl -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "hospedeId": 1,
    "imovelId": 1,
    "dataCheckIn": "2025-08-10",
    "dataCheckOut": "2025-08-15",
    "numeroHospedes": 2
  }'

# Consultar reserva
curl http://localhost:8080/api/reservas/1
```

---

**Próxima spec:** [spec004 — Hotel Core: Kafka Config e Eventos](spec004-hotelcore-kafka-config.md)
