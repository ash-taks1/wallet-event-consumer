# Event delivery latency

Run at: 2026-10-05T19:49:17.373Z

30 transactions created sequentially with the consumer running; lag = consumer receive time - event time (set inside the DB transaction, before commit).

| Check | Expected | Actual | Result |
|---|---|---|---|
| Events not delivered within 5 s | 0 | 0 | PASS |
| Max commit-to-consumer lag (ms) | < 2000 | max 93 | PASS |

HTTP latency (all requests): avg 7.7 ms, p95 18.9 ms, max 39.7 ms
Lag distribution: avg 68 ms, median 70 ms, p95 92 ms, max 93 ms.

**Overall: PASS**
