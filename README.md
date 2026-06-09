# Sistema de Gestão de Hotel — Arquitetura de Microsserviços

> API REST para gestão de reservas, hóspedes, imóveis e pagamentos, com comunicação assíncrona via Apache Kafka.
>
> Stack: **Spring Boot 3.x** + **Java 17+** + **Apache Kafka** + **PostgreSQL**

---

## Equipe

| Integrante | Responsabilidade |
|------------|-----------------|
| **Henrique** e **Gabriel** | Criação dos módulos de Hotel, Imóvel e Reserva (models, endpoints, regras de negócio) |
| **Yuri** | Implementação da integração com Apache Kafka (producers, consumers, configuração) |
| **Welington** | Conexão deste projeto com outros microsserviços e desenvolvimento de sistemas auxiliares |
| **Aratia** e **Júlia** | testes, testes unitários e de integração) |

---

## Visão Geral da Arquitetura

O sistema é composto por dois microsserviços desacoplados que se comunicam via Apache Kafka:

```
┌─────────────────────────┐          ┌──────────────────────────┐          ┌─────────────────────────┐
│      HOTEL CORE          │          │      APACHE KAFKA         │          │   PAGAMENTOS SERVICE     │
│  (porta 8080)            │          │   (Message Broker)        │          │  (porta 8081)            │
│                          │          │                           │          │                          │
│  Hóspedes                │  publica │  reserva.criada           │ consome  │  Processar pagamento     │
│  Imóveis / Quartos       │ ───────► │                           │ ───────► │  Gateway externo         │
│  Reservas                │  consome │  pagamento.resultado      │ publica  │  Estorno / reembolso     │
│  PostgreSQL              │ ◄─────── │                           │ ◄─────── │  PostgreSQL              │
└─────────────────────────┘          └──────────────────────────┘          └─────────────────────────┘
```

### Tópicos Kafka

| Tópico | Producer | Consumer | Descrição |
|--------|----------|----------|-----------|
| `reserva.criada` | Hotel Core | Pagamentos Service | Disparado ao criar uma reserva com status `PENDENTE` |
| `pagamento.resultado` | Pagamentos Service | Hotel Core | Retorna `APROVADO`, `RECUSADO` ou `ESTORNADO` |

---

## Módulos e Entidades

### Hóspede
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| nome | String | obrigatório |
| cpf | String | único |
| email | String | único |
| telefone | String | |
| dataNascimento | LocalDate | |
| dataCadastro | LocalDateTime | auto |

### Imóvel (Quarto/Unidade)
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| nome | String | ex: "Suíte 101" |
| tipo | Enum | STANDARD, LUXO, SUITE, APARTAMENTO |
| capacidade | Integer | nº de pessoas |
| precoDiaria | BigDecimal | |
| status | Enum | DISPONIVEL, OCUPADO, MANUTENCAO |

### Reserva
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| hospede | ManyToOne | → Hóspede |
| imovel | ManyToOne | → Imóvel |
| dataCheckIn | LocalDate | |
| dataCheckOut | LocalDate | |
| valorTotal | BigDecimal | calculado automaticamente |
| status | Enum | PENDENTE, CONFIRMADA, CANCELADA, CONCLUIDA |

### Pagamento
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long | PK |
| reserva | OneToOne | → Reserva |
| valor | BigDecimal | |
| metodo | Enum | PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO, BOLETO |
| status | Enum | PENDENTE, APROVADO, RECUSADO, ESTORNADO |

---

## Endpoints REST

### Hóspede
- `POST   /api/hospedes` — cadastrar
- `GET    /api/hospedes/{id}` — buscar
- `GET    /api/hospedes` — listar (paginado)
- `PUT    /api/hospedes/{id}` — atualizar
- `DELETE /api/hospedes/{id}` — remover

### Imóvel
- `POST   /api/imoveis`
- `GET    /api/imoveis?disponivel=true&dataInicio=...&dataFim=...`
- `PUT    /api/imoveis/{id}/status`

### Reserva
- `POST   /api/reservas` — criar (valida disponibilidade e dispara fluxo Kafka)
- `GET    /api/reservas/{id}`
- `PATCH  /api/reservas/{id}/cancelar`
- `GET    /api/reservas?hospedeId=...`

### Pagamento
- `POST   /api/pagamentos` — registrar
- `GET    /api/pagamentos/reserva/{reservaId}`

---

## Fluxo de uma Reserva

```
POST /api/reservas
       │
       ▼
 Valida disponibilidade
 Calcula valorTotal
 Salva Reserva (PENDENTE)
       │
       ├── publica ──► reserva.criada ──► Pagamentos Service consome
       │                                          │
       │                                   Processa pagamento
       │                                   Atualiza status
       │                                          │
       │               pagamento.resultado ◄── publica
       │                       │
       ▼                       ▼
                   Hotel Core consome
                   Reserva → CONFIRMADA ou CANCELADA
```

---

## Como Rodar

```bash
# Subir toda a infraestrutura (Kafka + PostgreSQL)
docker-compose up -d

# Swagger Hotel Core
http://localhost:8080/swagger-ui.html

# Swagger Pagamentos Service
http://localhost:8081/swagger-ui.html

# Kafka UI (visualizar tópicos e mensagens)
http://localhost:8090
```

### Exemplo de requisição

```bash
curl -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "hospedeId": 1,
    "imovelId": 1,
    "dataCheckIn": "2025-08-10",
    "dataCheckOut": "2025-08-15",
    "numeroHospedes": 2
  }'
```

---

## Imagens Docker

| Serviço | Imagem | Versão |
|---------|--------|--------|
| PostgreSQL | `postgres` | `16` |
| Kafka | `confluentinc/cp-kafka` | `7.6.0` |
| Zookeeper | `confluentinc/cp-zookeeper` | `7.6.0` |
| Kafka UI | `provectuslabs/kafka-ui` | `latest` |
