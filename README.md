# Planejamento: Sistema de Gestão de Hotel/Imóvel

> Stack principal: **Spring Boot** + Java 17+
> Tipo: API REST para gestão de reservas, hóspedes, imóveis e pagamentos

---

## 1. Estrutura do Projeto

```
src/main/java/com/seuprojeto/hotel
├── config/          → Configurações (Security, Swagger, etc)
├── controller/      → Endpoints REST
├── service/         → Regras de negócio
├── repository/      → Acesso ao banco (JPA)
├── model/           → Entidades JPA
├── dto/             → Objetos de transferência
├── mapper/          → Conversão Entity ↔ DTO
└── exception/       → Tratamento de erros
```

---

## 2. Modelagem das Entidades

### 🧑 Hospede

| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| nome | String | obrigatório |
| cpf | String | único |
| email | String | único |
| telefone | String | |
| dataNascimento | LocalDate | |
| endereco | String | ou entidade separada |
| dataCadastro | LocalDateTime | auto |

### 🏠 Imovel (Quarto/Unidade)

| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| nome | String | ex: "Suíte 101" |
| tipo | Enum | STANDARD, LUXO, SUITE, APARTAMENTO |
| descricao | String | |
| capacidade | Integer | nº de pessoas |
| precoDiaria | BigDecimal | |
| status | Enum | DISPONIVEL, OCUPADO, MANUTENCAO |
| comodidades | List\<String\> | wi-fi, ar, tv... |

### 📅 Reserva

| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| hospede | ManyToOne | → Hospede |
| imovel | ManyToOne | → Imovel |
| dataCheckIn | LocalDate | |
| dataCheckOut | LocalDate | |
| numeroHospedes | Integer | |
| valorTotal | BigDecimal | calculado |
| status | Enum | PENDENTE, CONFIRMADA, CANCELADA, CONCLUIDA |
| dataCriacao | LocalDateTime | auto |

### 💰 Pagamento

| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| reserva | OneToOne | → Reserva |
| valor | BigDecimal | |
| metodo | Enum | PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO, BOLETO |
| status | Enum | PENDENTE, APROVADO, RECUSADO, ESTORNADO |
| dataPagamento | LocalDateTime | |
| transacaoId | String | ID do gateway externo |

---

## 3. Relacionamentos

```
Hospede 1 ──── N Reserva N ──── 1 Imovel
                  │
                  │ 1
                  │
                  1
              Pagamento
```

---

## 4. Endpoints REST (mínimo viável)

### Hospede
- `POST   /api/hospedes` — cadastrar
- `GET    /api/hospedes/{id}` — buscar
- `GET    /api/hospedes` — listar (paginado)
- `PUT    /api/hospedes/{id}` — atualizar
- `DELETE /api/hospedes/{id}` — remover

### Imovel
- `POST   /api/imoveis`
- `GET    /api/imoveis?disponivel=true&dataInicio=...&dataFim=...`
- `PUT    /api/imoveis/{id}/status`

### Reserva
- `POST   /api/reservas` — criar (validar disponibilidade!)
- `GET    /api/reservas/{id}`
- `PATCH  /api/reservas/{id}/cancelar`
- `GET    /api/reservas?hospedeId=...`

### Pagamento
- `POST   /api/pagamentos` — registrar
- `GET    /api/pagamentos/reserva/{reservaId}`

---

## 5. Stack Recomendada

- **Spring Boot 3.x** + Java 17+
- **Spring Web** (REST)
- **Spring Data JPA** (ORM)
- **Spring Validation** (`@Valid`, `@NotNull`, etc)
- **PostgreSQL** (produção) / **H2** (dev)
- **Flyway** ou **Liquibase** (migrations)
- **Lombok** (menos boilerplate)
- **MapStruct** (DTOs)
- **Swagger/OpenAPI** (documentação)

---

## 6. Ordem de Desenvolvimento Sugerida

1. Setup do projeto (Spring Initializr)
2. Configurar banco + Flyway
3. CRUD de **Imovel** (mais simples, sem dependências)
4. CRUD de **Hospede**
5. **Reserva** com validação de disponibilidade
6. **Pagamento** integrado à reserva
7. Tratamento global de exceções (`@ControllerAdvice`)
8. Documentação Swagger
9. Testes (JUnit + Mockito)

---

## 💡 Dicas de Melhorias

### Regras de negócio críticas
- Validar **sobreposição de datas** ao criar reserva (query com `BETWEEN`)
- Calcular `valorTotal` automaticamente: `(checkOut - checkIn) × precoDiaria`
- Não permitir cancelar reserva já concluída
- Liberar imóvel automaticamente após checkout (job agendado com `@Scheduled`)

### Arquitetura
- Use **DTOs** sempre — nunca exponha entidades JPA direto no controller
- Adicione **Spring Security + JWT** (admin x recepcionista x cliente)
- Implemente **soft delete** (`deletedAt`) em vez de apagar registros
- Use **enums** pra status — evita strings mágicas
- Adicione `@Version` (lock otimista) na Reserva pra evitar overbooking em concorrência

### Funcionalidades extras que agregam valor
- Relatório de ocupação (taxa mensal, faturamento)
- Sistema de avaliações (estrelas + comentário do hóspede)
- Upload de fotos do imóvel (S3 ou Cloudinary)
- Integração com gateway real (Stripe, Mercado Pago, Asaas)
- Envio de email de confirmação (Spring Mail + template Thymeleaf)
- Dashboard com métricas (Spring Boot Actuator + Prometheus)

### Boas práticas técnicas
- Logs estruturados com **SLF4J**
- Cache com **Redis** pra consultas de disponibilidade
- Rate limiting (Bucket4j) nos endpoints públicos
- Testes de integração com **Testcontainers**

### Monetização
Esse sistema dá pra virar um **boilerplate vendável** no Gumroad — empacote com Docker Compose, README bom e Swagger pronto.

---

## 📋 Checklist de Implementação

- [ ] Criar projeto via Spring Initializr
- [ ] Configurar `application.yml` (dev e prod)
- [ ] Criar migrations Flyway
- [ ] Implementar entidades JPA
- [ ] Criar repositórios
- [ ] Criar DTOs e mappers
- [ ] Implementar services com regras de negócio
- [ ] Criar controllers REST
- [ ] Adicionar validações (`@Valid`)
- [ ] Tratamento global de exceções
- [ ] Configurar Swagger
- [ ] Escrever testes unitários
- [ ] Escrever testes de integração
- [ ] Dockerizar aplicação
- [ ] Deploy (Render, Railway, Fly.io)

---

*Documento criado para uso pessoal — adapte conforme a necessidade do projeto.*
