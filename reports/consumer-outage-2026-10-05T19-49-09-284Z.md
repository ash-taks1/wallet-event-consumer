# Consumer outage scenario

Run at: 2026-10-05T19:49:09.284Z

Consumer stopped for 35 s; 10 transactions were committed during the outage.
Messages waiting in the queue right before the restart: 10.
After the restart the consumer drained the queue.

| Check | Expected | Actual | Result |
|---|---|---|---|
| Transactions created while consumer was down | 10 | 10 | PASS |
| Events processed exactly once | 10 | 10 | PASS |
| Events missing (lost) | 0 | 0 | PASS |
| Events whose effect was applied more than once | 0 | 0 | PASS |

HTTP latency (all requests): avg 5.6 ms, p95 6.5 ms, max 40.2 ms
Each event waited 39.8-40.3 s between its commit and its processing, i.e. RabbitMQ kept it during the outage.

**Overall: PASS**
