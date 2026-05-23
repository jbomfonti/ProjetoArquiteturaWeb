# SPEC-010 — Testes

> **Atenção:** esta spec deve ser executada **somente após** todos os endpoints e consumers estarem implementados (spec001 a spec009).

---

## Imagens Docker (para testes de integração)

| Serviço    | Imagem                    | Versão  |
|------------|---------------------------|---------|
| PostgreSQL | `postgres`                | `16`    |
| Kafka      | `confluentinc/cp-kafka`   | `7.6.0` |
| Zookeeper  | `confluentinc/cp-zookeeper` | `7.6.0` |

> Testes de integração usam **Testcontainers** — os containers sobem automaticamente durante o teste, sem precisar de infraestrutura manual.

---

## Dependências de teste

```xml
<!-- JUnit 5 + Spring Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Kafka Test -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers (PostgreSQL + Kafka) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Organização dos testes

```
hotel-core/src/test/java/com/hotel/core/
├── unit/
│   ├── ReservaServiceTest.java          ← lógica de negócio (sem Kafka/DB)
│   └── ReservaProducerTest.java         ← producer isolado
├── integration/
│   ├── ReservaControllerIT.java         ← endpoint POST /api/reservas
│   └── PagamentoResultadoConsumerIT.java ← consumer recebe evento e atualiza reserva

pagamentos-service/src/test/java/com/hotel/pagamentos/
├── unit/
│   └── PagamentoServiceTest.java        ← lógica de processamento + idempotência
└── integration/
    └── ReservaCriadaConsumerIT.java     ← consumer recebe evento e publica resultado
```

---

## Testes unitários — Hotel Core

### ReservaServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock ReservaRepository reservaRepository;
    @Mock HospedeRepository hospedeRepository;
    @Mock ImovelRepository imovelRepository;
    @Mock ReservaProducer reservaProducer;

    @InjectMocks ReservaService reservaService;

    @Test
    void deveCriarReservaComStatusPendente() {
        Hospede hospede = new Hospede(); hospede.setId(1L);
        Imovel imovel = new Imovel();
        imovel.setId(1L);
        imovel.setDisponivel(true);
        imovel.setPrecoPorNoite(new BigDecimal("200.00"));

        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CriarReservaRequest request = new CriarReservaRequest(
            1L, 1L,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(6),
            2
        );

        ReservaResponse response = reservaService.criarReserva(request);

        assertThat(response.status()).isEqualTo("PENDENTE");
        assertThat(response.valorTotal()).isEqualByComparingTo("1000.00"); // 5 noites × 200
        verify(reservaProducer).publicar(any());
    }

    @Test
    void deveLancarExcecaoQuandoImovelIndisponivel() {
        Imovel imovel = new Imovel();
        imovel.setDisponivel(false);

        when(hospedeRepository.findById(any())).thenReturn(Optional.of(new Hospede()));
        when(imovelRepository.findById(any())).thenReturn(Optional.of(imovel));

        assertThatThrownBy(() ->
            reservaService.criarReserva(
                new CriarReservaRequest(1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 1)
            )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("indisponível");
    }

    @Test
    void deveLancarExcecaoEmConflitoDeDatas() {
        Imovel imovel = new Imovel();
        imovel.setDisponivel(true);
        imovel.setPrecoPorNoite(BigDecimal.TEN);

        when(hospedeRepository.findById(any())).thenReturn(Optional.of(new Hospede()));
        when(imovelRepository.findById(any())).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
            reservaService.criarReserva(
                new CriarReservaRequest(1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 1)
            )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("período");
    }
}
```

---

## Testes unitários — Pagamentos Service

### PagamentoServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock PagamentoRepository pagamentoRepository;
    @Mock GatewayPagamento gateway;
    @Mock PagamentoResultadoProducer resultadoProducer;

    @InjectMocks PagamentoService pagamentoService;

    @Test
    void deveProcessarPagamentoAprovado() {
        when(pagamentoRepository.existsByReservaId(1L)).thenReturn(false);
        when(pagamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gateway.processar(any(), any()))
            .thenReturn(new GatewayPagamento.ResultadoGateway(true, null));

        ReservaCriadaEvent evento = new ReservaCriadaEvent(
            1L, 1L, 1L,
            LocalDate.now(), LocalDate.now().plusDays(3),
            new BigDecimal("600.00")
        );

        pagamentoService.processar(evento);

        verify(pagamentoRepository, times(2)).save(any());
        verify(resultadoProducer).publicar(argThat(p -> p.getStatus() == StatusPagamento.APROVADO));
    }

    @Test
    void deveIgnorarEventoDuplicado() {
        when(pagamentoRepository.existsByReservaId(1L)).thenReturn(true);

        pagamentoService.processar(
            new ReservaCriadaEvent(1L, 1L, 1L, LocalDate.now(), LocalDate.now().plusDays(1), BigDecimal.TEN)
        );

        verifyNoInteractions(gateway, resultadoProducer);
        verify(pagamentoRepository, never()).save(any());
    }
}
```

---

## Testes de integração — Hotel Core

### ReservaControllerIT.java

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReservaControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("hoteldb")
            .withUsername("hotel")
            .withPassword("hotel123");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired TestRestTemplate restTemplate;
    @Autowired ReservaRepository reservaRepository;

    @Test
    void deveCriarReservaERetornarPendente() {
        // pré-condição: hospede e imovel precisam existir no banco
        // (inserir via repository ou SQL antes do teste)

        CriarReservaRequest request = new CriarReservaRequest(
            1L, 1L,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(4),
            2
        );

        ResponseEntity<ReservaResponse> response =
            restTemplate.postForEntity("/api/reservas", request, ReservaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().status()).isEqualTo("PENDENTE");
        assertThat(response.getBody().id()).isNotNull();
    }
}
```

---

## Ordem de execução recomendada

1. Testes unitários (`mvn test -Dtest="*Test"`)
2. Testes de integração (`mvn verify -Dtest="*IT"`)
3. Testes E2E manuais com `docker-compose up -d` + curl (exemplos em [spec003](spec003-hotelcore-endpoints.md))

---

## Cobertura mínima esperada

| Camada        | Target |
|---------------|--------|
| Service       | 80%    |
| Controller    | 70%    |
| Consumer      | 70%    |
| Producer      | 60%    |

---

**Esta é a última spec.** Retorne ao índice: [SPECS-INDEX.md](SPECS-INDEX.md)
