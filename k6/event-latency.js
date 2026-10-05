// Section 6.5: in normal operation (no outage) an event must reach the consumer within 2 seconds
// of the transaction being committed. Creates transactions one by one and reads the lag the
// consumer recorded for each event (received_at - occurred_at, occurred_at is set before commit).
//
// Run (both stacks up): scripts/k6.sh event-latency.js
import { sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createUser, deposit, processedEvents, REPORT_DIR, transfer, withdraw } from './lib/api.js';
import { buildReport, reportFiles } from './lib/report.js';

const TRANSACTIONS = Number(__ENV.TRANSACTIONS || 30);

const lag = new Trend('event_lag_ms');
const missing = new Counter('events_not_delivered_within_5s');

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    event_lag_ms: ['max<2000'],
    events_not_delivered_within_5s: ['count==0'],
  },
};

export default function () {
  missing.add(0);
  const alice = createUser('latency-alice');
  const bob = createUser('latency-bob');
  deposit(alice, 1000000);
  for (let i = 0; i < TRANSACTIONS; i++) {
    const kind = i % 3;
    const tx = kind === 0 ? deposit(bob, 100) : kind === 1 ? withdraw(alice, 100) : transfer(alice, bob.walletId, 100);
    const deadline = Date.now() + 5000;
    let events = processedEvents(tx.transactionId);
    while ((events === null || events.length === 0) && Date.now() < deadline) {
      sleep(0.05);
      events = processedEvents(tx.transactionId);
    }
    if (events && events.length > 0) {
      lag.add(events[0].lagMs);
    } else {
      missing.add(1);
    }
  }
}

export function handleSummary(data) {
  const l = data.metrics.event_lag_ms ? data.metrics.event_lag_ms.values : null;
  const report = buildReport(data, 'Event delivery latency',
    [`${TRANSACTIONS} transactions created sequentially with the consumer running; lag = consumer receive time - event time (set inside the DB transaction, before commit).`],
    [
      { label: 'Events not delivered within 5 s', metric: 'events_not_delivered_within_5s', expected: 0 },
      { label: 'Max commit-to-consumer lag (ms)', metric: 'event_lag_ms', expected: '< 2000' },
    ],
    l ? [`Lag distribution: avg ${l.avg.toFixed(0)} ms, median ${l.med.toFixed(0)} ms, p95 ${l['p(95)'].toFixed(0)} ms, max ${l.max.toFixed(0)} ms.`] : []);
  return reportFiles(REPORT_DIR, 'event-latency', report, data);
}
