# Índice de Specs — Sistema Hotel + Kafka

> Ordem de implementação recomendada. Os testes (spec010) devem ser feitos **somente após** os endpoints estarem criados.

---

| Spec | Título | Serviço | Depende de |
|------|--------|---------|------------|
| [spec001](spec001-infraestrutura-docker.md) | Infraestrutura: Docker Compose (Kafka + PostgreSQL) | Infra | — |
| [spec002](spec002-hotelcore-models.md) | Hotel Core: Models e Banco de Dados | hotel-core | spec001 |
| [spec003](spec003-hotelcore-endpoints.md) | Hotel Core: REST Endpoints | hotel-core | spec002 |
| [spec004](spec004-hotelcore-kafka-config.md) | Hotel Core: Kafka Config e Eventos | hotel-core | spec001 |
| [spec005](spec005-hotelcore-producer.md) | Hotel Core: Kafka Producer | hotel-core | spec003, spec004 |
| [spec006](spec006-hotelcore-consumer.md) | Hotel Core: Kafka Consumer | hotel-core | spec004 |
| [spec007](spec007-pagamentos-models.md) | Pagamentos Service: Models e Banco | pagamentos-service | spec001 |
| [spec008](spec008-pagamentos-consumer-gateway.md) | Pagamentos Service: Consumer e Gateway | pagamentos-service | spec007 |
| [spec009](spec009-pagamentos-producer.md) | Pagamentos Service: Kafka Producer | pagamentos-service | spec008 |
| [spec010](spec010-testes.md) | Testes *(após endpoints)* | ambos | spec001–spec009 |

---

## Imagens Docker (referência rápida)

| Serviço    | Imagem                      | Versão  |
|------------|-----------------------------|---------|
| PostgreSQL | `postgres`                  | `16`    |
| Kafka      | `confluentinc/cp-kafka`     | `7.6.0` |
| Zookeeper  | `confluentinc/cp-zookeeper` | `7.6.0` |
| Kafka UI   | `provectuslabs/kafka-ui`    | `latest` |

---

## Fluxo macro

```
spec001 → spec002 → spec003 ─┐
                              ├─► spec005 ─┐
                   spec004 ──┘             │
                                           ▼
spec007 → spec008 → spec009           [sistema rodando]
                                           │
                                       spec010 (testes)
```
