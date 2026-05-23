# Arquitetura com Kafka: Hotel Core ↔ Pagamentos

> Sistema de dois microsserviços desacoplados que se comunicam via Apache Kafka.

---

## Diagrama da Arquitetura

```
┌─────────────────────────┐          ┌──────────────────────────┐          ┌─────────────────────────┐
│      HOTEL CORE          │          │      APACHE KAFKA         │          │   PAGAMENTOS SERVICE     │
│  (porta 8080)            │          │   (Message Broker)        │          │  (porta 8081)            │
│                          │          │                           │          │                          │
│  ┌────────────────────┐  │          │  ┌─────────────────────┐  │          │  ┌────────────────────┐  │
│  │     Hóspedes       │  │          │  │   reserva.criada    │  │          │  │ Processar pagamento│  │
│  ├────────────────────┤  │  publica │  └─────────────────────┘  │ consome  │  ├────────────────────┤  │
│  │  Imóveis / Quartos │  │ ───────► │                           │ ───────► │  │ Estorno / reembolso│  │
│  ├────────────────────┤  │          │  ┌─────────────────────┐  │          │  ├────────────────────┤  │
│  │     Reservas       │  │ consome  │  │ pagamento.resultado  │  │  publica │  │  Gateway externo   │  │
│  ├────────────────────┤  │ ◄─────── │  └─────────────────────┘  │ ◄─────── │  ├────────────────────┤  │
│  │    PostgreSQL      │  │          │                           │          │  │    PostgreSQL      │  │
│  └────────────────────┘  │          └──────────────────────────┘          │  └────────────────────┘  │
└─────────────────────────┘                                                  └─────────────────────────┘
```

---

## Tópicos Kafka

| Tópico               | Producer           | Consumer               | Descrição                                    |
|----------------------|--------------------|------------------------|----------------------------------------------|
| `reserva.criada`     | Hotel Core         | Pagamentos Service     | Disparado ao criar uma reserva com status `PENDENTE` |
| `pagamento.resultado`| Pagamentos Service | Hotel Core             | Retorna `APROVADO`, `RECUSADO` ou `ESTORNADO`|

---

## Fluxo de Eventos

### Fluxo feliz (pagamento aprovado)

```
Hóspede / API                Hotel Core              Kafka              Pagamentos Service
─────────────                ──────────              ─────              ──────────────────

POST /api/reservas
        │
        ▼
  Valida disponibilidade
  Calcula valorTotal
  Salva Reserva (PENDENTE)
        │
        ├── publica ──────► reserva.criada ──────► consome
        │                                                │
        │                                         Verifica idempotência
        │                                         Salva Pagamento (PENDENTE)
        │                                         Chama Gateway Externo
        │                                                │
        │                                         Gateway retorna APROVADO
        │                                         Atualiza Pagamento → APROVADO
        │                                                │
        │                   pagamento.resultado ◄── publica
        │                         │
        ▼                         ▼
                            consome
                            Atualiza Reserva → CONFIRMADA
```

---

### Fluxo de recusa (pagamento recusado)

```
Hóspede / API                Hotel Core              Kafka              Pagamentos Service
─────────────                ──────────              ─────              ──────────────────

POST /api/reservas
        │
        ▼
  Salva Reserva (PENDENTE)
        │
        ├── publica ──────► reserva.criada ──────► consome
        │                                                │
        │                                         Gateway retorna RECUSADO
        │                                         Atualiza Pagamento → RECUSADO
        │                                                │
        │                   pagamento.resultado ◄── publica (status=RECUSADO, motivo=...)
        │                         │
        ▼                         ▼
                            consome
                            Atualiza Reserva → CANCELADA
```

---

## Mapeamento de Status

### Reserva (Hotel Core)

| Status       | Descrição                                      |
|--------------|------------------------------------------------|
| `PENDENTE`   | Reserva criada, aguardando confirmação do pagamento |
| `CONFIRMADA` | Pagamento aprovado pelo gateway                |
| `CANCELADA`  | Pagamento recusado, estornado ou cancelamento manual |
| `CONCLUIDA`  | Hóspede realizou check-out                     |

### Pagamento (Pagamentos Service)

| Status      | Descrição                          |
|-------------|------------------------------------|
| `PENDENTE`  | Aguardando resposta do gateway     |
| `APROVADO`  | Gateway confirmou o pagamento      |
| `RECUSADO`  | Gateway recusou (limite, dados, etc.) |
| `ESTORNADO` | Pagamento revertido após aprovação |

### Transição de status via Kafka

```
pagamento.resultado.status  →  reserva.status
────────────────────────────────────────────
APROVADO                    →  CONFIRMADA
RECUSADO                    →  CANCELADA
ESTORNADO                   →  CANCELADA
```

---

## Garantias e Boas Práticas

### Idempotência
O `PagamentoService` verifica `existsByReservaId()` antes de processar.  
Mensagens duplicadas (redelivery do Kafka) são ignoradas com segurança.

### Retry + Dead Letter Topic (DLT)
Configurado com `FixedBackOff(2000ms, 3 tentativas)`.  
Após esgotar as tentativas, a mensagem vai para `reserva.criada.DLT` ou `pagamento.resultado.DLT` automaticamente.

### Lock otimista
A entidade `Reserva` usa `@Version` para evitar overbooking em cenários de concorrência.

### Chave de partição Kafka
Tanto o producer do Hotel Core quanto o do Pagamentos Service usam `reservaId.toString()` como chave.  
Isso garante que todos os eventos de uma mesma reserva sempre caiam na mesma partição, preservando a ordem.

---

## Rodando o projeto

```bash
# Subir toda a infraestrutura
docker-compose up -d

# Verificar logs do Hotel Core
docker logs -f hotel-core

# Verificar logs do Pagamentos Service
docker logs -f pagamentos-service

# Acessar Kafka UI (visualizar tópicos e mensagens)
open http://localhost:8090

# Swagger Hotel Core
open http://localhost:8080/swagger-ui.html

# Swagger Pagamentos Service
open http://localhost:8081/swagger-ui.html
```

### Exemplo de requisição

```bash
# Criar uma reserva (dispara o fluxo Kafka completo)
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

Após a requisição, o fluxo completo acontece de forma assíncrona:
1. Hotel Core salva a reserva como `PENDENTE` e publica em `reserva.criada`
2. Pagamentos Service consome, processa e publica em `pagamento.resultado`
3. Hotel Core consome e atualiza a reserva para `CONFIRMADA` (ou `CANCELADA`)

---

## Estrutura dos Projetos

```
hotel-core/
├── src/main/java/com/hotel/core/
│   ├── config/          KafkaConfig.java
│   ├── controller/      ReservaController.java, HospedeController.java
│   ├── service/         ReservaService.java
│   ├── repository/      ReservaRepository.java
│   ├── model/           Reserva.java, Hospede.java, Imovel.java
│   ├── dto/             CriarReservaRequest.java, ReservaResponse.java
│   ├── event/           ReservaCriadaEvent.java, PagamentoResultadoEvent.java
│   ├── kafka/
│   │   ├── producer/    ReservaProducer.java
│   │   └── consumer/    PagamentoResultadoConsumer.java
│   └── exception/       GlobalExceptionHandler.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/    V1__create_tables.sql

pagamentos-service/
├── src/main/java/com/hotel/pagamentos/
│   ├── config/          KafkaConfig.java
│   ├── service/         PagamentoService.java
│   ├── repository/      PagamentoRepository.java
│   ├── model/           Pagamento.java
│   ├── event/           ReservaCriadaEvent.java, PagamentoResultadoEvent.java
│   ├── gateway/         GatewayPagamento.java, MockGatewayPagamento.java
│   ├── kafka/
│   │   ├── producer/    PagamentoResultadoProducer.java
│   │   └── consumer/    ReservaCriadaConsumer.java
│   └── exception/       GlobalExceptionHandler.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/    V1__create_pagamentos.sql
```
