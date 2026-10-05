# wallet-event-consumer

Consumes the transaction events published by `wallet-service` and records each one exactly once.

For every event it receives from RabbitMQ, it writes one row to the `processed_events` table and logs one
line. That row is the visible effect of the event. It also makes sure a redelivered event is never applied
twice.

Tech: Java 21, Spring Boot 4, RabbitMQ, PostgreSQL, k6.

## Running

Start the `wallet-service` stack first, then:

```bash
docker compose up -d --build
```

This service has no infrastructure of its own. It joins the `digital-wallet` Docker network created by
wallet-service and uses that stack's RabbitMQ, and its own database (`consumer`) on the same PostgreSQL server.

| Service | Address |
|---|---|
| API | http://localhost:8091/api/v1 |
| Swagger UI | http://localhost:8091/swagger-ui.html |
| PostgreSQL | localhost:5440, database `consumer` (consumer / consumer) |

API:

- `GET /api/v1/processed-events?transactionId=<id>` returns the processed event of a transaction, with its
  delay (`lagMs`).
- `GET /api/v1/stats` returns the total number of processed events.

## How it works

- The queue `wallet-event-consumer.transactions` is a durable quorum queue bound to the `wallet.events`
  exchange. RabbitMQ keeps the events while this service is down.
- `event_id` is unique in `processed_events`. The row is saved in a database transaction, and the message is
  acknowledged only after that transaction commits:
  - if the same event arrives again, its id is already there, so it is skipped;
  - if two copies arrive at the same moment, the second insert hits the unique constraint and is skipped.
- A message that keeps failing is retried a few times and then moved to the dead-letter queue
  `wallet-event-consumer.transactions.dlq`.
- The event carries the trace id of the original HTTP request, so the consumer's log lines share the same
  `traceId` as wallet-service. One search in Kibana shows the full path.

## Scenarios

Both stacks must be running.

```bash
scripts/consumer-outage.sh          # consumer stopped for 35 s, transactions meanwhile, then verified
scripts/k6.sh event-latency.js      # event delay must stay under 2 seconds
```

The outage script stops the consumer and creates 10 transactions through wallet-service. It shows that the
events are waiting in RabbitMQ, keeps the consumer down for at least 30 seconds, then starts it again. Then
it checks that every transaction was processed exactly once, with none lost and none applied twice.

Reports are written to `reports/`.

## Tests

```bash
./mvnw test
```

Unit tests cover the processor: a new event is recorded, a known event is skipped, and losing a concurrent
insert counts as a duplicate.
