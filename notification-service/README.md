# Notification Service

Consumes mortgage application events from Kafka (`loan.applications`) and sends applicant notifications.

## Features

- Kafka consumer for `CREATE`, `UPDATE`, and `DELETE` application events
- **Exactly-once processing** via `processed_events` table (topic + partition + offset)
- **Business idempotency** via `notification_logs` (application + event type + correlation ID)
- **Retries** with exponential backoff before sending to DLQ
- **DLQ** topic `loan.applications.dlq` for poison/failed messages
- Structured application logging for every notification (`notification.received`, `notification.prepared`, `notification.recorded`, etc.)
- Logging channel by default (no SMTP required for local dev)
- Optional email delivery via Spring Mail
- REST API to inspect sent notifications

## Run Locally (H2 + Kafka)

```bash
cd notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Service runs on `http://localhost:8081`.

## Run with Docker Compose

```bash
cd infra
docker-compose up --build notification-service
```

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/notifications` | Paginated notification log |
| GET | `/actuator/health` | Health check |

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker |
| `NOTIFICATION_EMAIL_ENABLED` | `false` | Enable SMTP email delivery |
| `NOTIFICATION_EMAIL_FROM` | `noreply@bank.com` | Sender address |
| `SERVER_PORT` | `8081` | HTTP port |

### Kafka retry + DLQ

| Property | Default | Description |
|----------|---------|-------------|
| `notification.kafka.dlq-topic` | `loan.applications.dlq` | Dead letter topic |
| `notification.kafka.retry.initial-interval-ms` | `1000` | First retry delay |
| `notification.kafka.retry.multiplier` | `2.0` | Backoff multiplier |
| `notification.kafka.retry.max-interval-ms` | `10000` | Max delay between retries |
| `notification.kafka.retry.max-elapsed-ms` | `30000` | Total retry window before DLQ |

## Processing guarantees

### Exactly-once processing

Two layers of idempotency:

| Layer | Mechanism | Prevents |
|-------|-----------|----------|
| Kafka EOS | `processed_events` table keyed by `topic + partition + offset` | Re-processing the same message after redelivery |
| Business | `notification_logs` keyed by `application_id + event_type + correlation_id` (SENT/SKIPPED) | Duplicate notifications from republished events |

On delivery failure, the service throws `NotificationDeliveryException` and rolls back the DB transaction — nothing is marked processed, so retries work correctly.

### Retries + DLQ flow

```
loan.applications
       │
       ▼
  consume event
       │
       ├── success ──► notification_logs + processed_events
       │
       └── failure ──► exponential backoff retries (up to max-elapsed-ms)
                              │
                              └── exhausted ──► loan.applications.dlq
```

Inspect failed messages in Kafka UI under topic `loan.applications.dlq`.

## Notification logging

Every notification is logged to the application log with structured fields:

| Log event | When |
|-----------|------|
| `notification.received` | Kafka event consumed |
| `notification.skipped` | Duplicate offset or business idempotency hit |
| `notification.prepared` | Notification content built (SENT or SKIPPED) |
| `notification.delivered` | Channel dispatched (log or email) |
| `notification.recorded` | Persisted to `notification_logs` table |
| `notification.failed` | Delivery error before retry/DLQ |

View persisted notifications via API:

```bash
curl http://localhost:8081/api/v1/notifications
```

Tail live logs in Docker:

```bash
docker logs mortgage-notification-service -f
```

## Inspect Kafka

```bash
# List topics
docker exec -it mortgage-kafka kafka-topics --list --bootstrap-server localhost:9092

# View application events
docker exec -it mortgage-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic loan.applications \
  --from-beginning

# View dead-letter messages
docker exec -it mortgage-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic loan.applications.dlq \
  --from-beginning
```

Or use Kafka UI at `http://localhost:8085`.

## Event Handling

| Event | Status | Notification |
|-------|--------|--------------|
| CREATE | PENDING | Application received |
| UPDATE | PENDING | Application updated |
| UPDATE | APPROVED | Application approved |
| UPDATE | REJECTED | Application rejected |
| DELETE | — | Application cancelled |
