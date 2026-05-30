# SPEC-001 — Infraestrutura: Docker Compose

## O que essa spec faz

Sobe toda a infraestrutura necessária para os dois microsserviços rodarem:

| Container | Porta | Para quê |
|---|---|---|
| `postgres-hotel` | 5432 | Banco do hotel-core |
| `postgres-pagamentos` | 5433 | Banco do pagamentos-service |
| `kafka` | 9092 | Mensageria entre os serviços |
| `zookeeper` | 2181 | Necessário para o Kafka 7.x funcionar |
| `kafka-ui` | 8090 | Interface web para ver as mensagens |

---

## docker-compose.yml

```yaml
version: '3.8'

services:

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

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

## Tópicos Kafka

| Tópico | Partições | Quem publica → Quem consome |
|---|---|---|
| `reserva.criada` | 3 | hotel-core → pagamentos-service |
| `reserva.criada.DLT` | 1 | mensagens com erro |
| `pagamento.resultado` | 3 | pagamentos-service → hotel-core |
| `pagamento.resultado.DLT` | 1 | mensagens com erro |

---

## Comandos

```bash
docker-compose up -d       # sobe tudo
docker-compose ps          # status dos containers
docker-compose down        # para tudo
docker-compose down -v     # para e apaga os volumes (reset total)
```

Após subir, acesse:
- **Kafka UI:** http://localhost:8090
- **Banco hotel:** `localhost:5432` → db `hoteldb`, user `hotel`
- **Banco pagamentos:** `localhost:5433` → db `pagamentosdb`, user `pagamentos`

---

## Dependências no pom.xml (ambos os serviços)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

---

**Próxima spec:** [spec002 — Hotel Core: Models e Banco](spec002-hotelcore-models.md)
