# SPEC-001 — Infraestrutura: Docker Compose (Kafka + PostgreSQL)

---

## Imagens Docker

| Serviço        | Imagem oficial                          | Versão recomendada |
|----------------|-----------------------------------------|--------------------|
| PostgreSQL     | `postgres`                              | `postgres:16`      |
| Kafka (broker) | `confluentinc/cp-kafka`                 | `7.6.0`            |
| Zookeeper      | `confluentinc/cp-zookeeper`             | `7.6.0`            |
| Kafka UI       | `provectuslabs/kafka-ui`                | `latest`           |

```yaml
# Puxar manualmente (opcional)
docker pull postgres:16
docker pull confluentinc/cp-zookeeper:7.6.0
docker pull confluentinc/cp-kafka:7.6.0
docker pull provectuslabs/kafka-ui:latest
```

---

## Objetivo

Subir toda a infraestrutura necessária para os dois microsserviços:

- **hotel-core** (porta 8080) — PostgreSQL + produtor/consumidor Kafka
- **pagamentos-service** (porta 8081) — PostgreSQL separado + produtor/consumidor Kafka
- **Apache Kafka** + Zookeeper (porta 9092)
- **Kafka UI** (porta 8090) para visualização de tópicos

---

## Tópicos Kafka a criar

| Tópico                  | Partições | Replicação |
|-------------------------|-----------|------------|
| `reserva.criada`        | 3         | 1          |
| `reserva.criada.DLT`    | 1         | 1          |
| `pagamento.resultado`   | 3         | 1          |
| `pagamento.resultado.DLT` | 1       | 1          |

---

## Implementação: docker-compose.yml

```yaml
version: '3.8'

services:

  # ─────────────────────────────────────────────
  # Zookeeper (necessário para Kafka 7.x)
  # ─────────────────────────────────────────────
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  # ─────────────────────────────────────────────
  # Apache Kafka
  # ─────────────────────────────────────────────
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  # ─────────────────────────────────────────────
  # Kafka UI
  # ─────────────────────────────────────────────
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092

  # ─────────────────────────────────────────────
  # PostgreSQL — Hotel Core
  # ─────────────────────────────────────────────
  postgres-hotel:
    image: postgres:16
    container_name: postgres-hotel
    environment:
      POSTGRES_DB: hoteldb
      POSTGRES_USER: hotel
      POSTGRES_PASSWORD: hotel123
    ports:
      - "5432:5432"
    volumes:
      - postgres_hotel_data:/var/lib/postgresql/data

  # ─────────────────────────────────────────────
  # PostgreSQL — Pagamentos Service
  # ─────────────────────────────────────────────
  postgres-pagamentos:
    image: postgres:16
    container_name: postgres-pagamentos
    environment:
      POSTGRES_DB: pagamentosdb
      POSTGRES_USER: pagamentos
      POSTGRES_PASSWORD: pagamentos123
    ports:
      - "5433:5432"
    volumes:
      - postgres_pagamentos_data:/var/lib/postgresql/data

volumes:
  postgres_hotel_data:
  postgres_pagamentos_data:
```

---

## Comandos úteis

```bash
# Subir tudo em background
docker-compose up -d

# Ver status dos containers
docker-compose ps

# Verificar logs do Kafka
docker logs -f kafka

# Parar tudo
docker-compose down

# Parar e remover volumes (reset total)
docker-compose down -v
```

---

## Verificação

Após `docker-compose up -d`, acesse:

- Kafka UI: http://localhost:8090
- PostgreSQL Hotel Core: `localhost:5432` (db: `hoteldb`, user: `hotel`)
- PostgreSQL Pagamentos: `localhost:5433` (db: `pagamentosdb`, user: `pagamentos`)

---

## Dependências no pom.xml (ambos os serviços)

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Flyway para migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

---

**Próxima spec:** [spec002 — Hotel Core: Models e Banco de Dados](spec002-hotelcore-models.md)
