# SPEC-010 — Testes

> **Execute esta spec somente após** as specs 001 a 009 estarem implementadas.

## O que essa spec cobre

Testes unitários e de integração para os dois serviços. Testes de integração usam **Testcontainers** — os containers sobem automaticamente, sem precisar do docker-compose rodando.

---

## Dependências (pom.xml de ambos os serviços)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
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
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Organização

```
hotel-core/src/test/
└── unit/
    ├── ReservaServiceTest.java           ← regras de negócio (sem Kafka/DB)
    └── ReservaProducerTest.java          ← producer isolado
└── integration/
    ├── ReservaControllerIT.java          ← POST /api/reservas
    └── PagamentoResultadoConsumerIT.java ← consumer atualiza reserva

pagamentos-service/src/test/
└── unit/
    └── PagamentoServiceTest.java         ← processamento + idempotência
└── integration/
    └── ReservaCriadaConsumerIT.java      ← consumer processa e publica resultado
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
        imovel.setId(1L); imovel.setDisponivel(true);
        imovel.setPrecoPorNoite(new BigDecimal("200.00"));

        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(imovelRepository.findById(1L)).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReservaResponse response = reservaService.criarReserva(
            new CriarReservaRequest(1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(6), 2)
        );

        assertThat(response.status()).isEqualTo("PENDENTE");
        assertThat(response.valorTotal()).isEqualByComparingTo("1000.00"); // 5 noites × 200
        verify(reservaProducer).publicar(any());
    }

    @Test
    void deveLancarExcecaoQuandoImovelIndisponivel() {
        Imovel imovel = new Imovel(); imovel.setDisponivel(false);
        when(hospedeRepository.findById(any())).thenReturn(Optional.of(new Hospede()));
        when(imovelRepository.findById(any())).thenReturn(Optional.of(imovel));

        assertThatThrownBy(() ->
            reservaService.criarReserva(
                new CriarReservaRequest(1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 1)
            )
        ).isInstanceOf(IllegalStateException.class).hasMessageContaining("indisponível");
    }

    @Test
    void deveLancarExcecaoEmConflitoDeDatas() {
        Imovel imovel = new Imovel(); imovel.setDisponivel(true); imovel.setPrecoPorNoite(BigDecimal.TEN);
        when(hospedeRepository.findById(any())).thenReturn(Optional.of(new Hospede()));
        when(imovelRepository.findById(any())).thenReturn(Optional.of(imovel));
        when(reservaRepository.existeConflitoDeDatas(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
            reservaService.criarReserva(
                new CriarReservaRequest(1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 1)
            )
        ).isInstanceOf(IllegalStateException.class).hasMessageContaining("período");
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
        when(gateway.processar(any(), any())).thenReturn(new GatewayPagamento.ResultadoGateway(true, null));

        pagamentoService.processar(new ReservaCriadaEvent(
            1L, 1L, 1L, LocalDate.now(), LocalDate.now().plusDays(3), new BigDecimal("600.00")
        ));

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

## Teste de integração — Hotel Core

### ReservaControllerIT.java

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReservaControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("hoteldb").withUsername("hotel").withPassword("hotel123");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

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
        // pré-condição: hospede e imóvel precisam existir no banco
        CriarReservaRequest request = new CriarReservaRequest(
            1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), 2
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

## Como rodar

```bash
# Só unitários
mvn test -Dtest="*Test"

# Unitários + integração (sobe Testcontainers automaticamente)
mvn verify -Dtest="*IT"

# Teste E2E manual (requer docker-compose up -d)
# use os exemplos de curl da spec003
```

---

## Cobertura mínima esperada

| Camada | Target |
|---|---|
| Service | 80% |
| Controller | 70% |
| Consumer | 70% |
| Producer | 60% |

---

**Todas as specs concluídas.** Retorne ao índice: [SPECS-INDEX.md](SPECS-INDEX.md)
