// Outage scenario, step 2 (consumer restarted): every transaction created during the outage must be
// processed exactly once: exactly one processed_events row per transaction, nothing missing, nothing twice.
// Driven by scripts/consumer-outage.sh.
import { sleep } from 'k6';
import { Counter, Gauge, Trend } from 'k6/metrics';
import { processedEvents, REPORT_DIR } from './lib/api.js';
import { buildReport, reportFiles } from './lib/report.js';

const produced = JSON.parse(open(__ENV.TX_FILE || '/reports/outage-transactions.json'));
const TIMEOUT_SECONDS = Number(__ENV.TIMEOUT_SECONDS || 60);
const expectedCount = produced.transactions.length;

const expectedEvents = new Gauge('events_expected');
const processedOnce = new Counter('events_processed_exactly_once');
const missing = new Counter('events_missing');
const duplicatedEffect = new Counter('events_with_duplicated_effect');
const waitedInBroker = new Trend('outage_event_wait_ms');

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    events_processed_exactly_once: [`count==${expectedCount}`],
    events_missing: ['count==0'],
    events_with_duplicated_effect: ['count==0'],
  },
};

export default function () {
  [processedOnce, missing, duplicatedEffect].forEach((c) => c.add(0));
  expectedEvents.add(expectedCount);

  // Wait until the consumer has caught up with every event (or the timeout expires).
  const deadline = Date.now() + TIMEOUT_SECONDS * 1000;
  let pending = produced.transactions;
  while (pending.length > 0 && Date.now() < deadline) {
    pending = pending.filter((tx) => {
      const events = processedEvents(tx.transactionId);
      return events === null || events.length === 0;
    });
    if (pending.length > 0) sleep(0.5);
  }

  produced.transactions.forEach((tx) => {
    const events = processedEvents(tx.transactionId) || [];
    if (events.length === 0) {
      missing.add(1);
      console.error(`MISSING: ${tx.type} ${tx.transactionId}`);
      return;
    }
    if (events.length === 1) processedOnce.add(1);
    if (events.length > 1) duplicatedEffect.add(1);
    waitedInBroker.add(events[0].lagMs);
    console.log(`${tx.type.padEnd(10)} ${tx.transactionId}: processed=${events.length} lagMs=${events[0].lagMs}`);
  });
}

export function handleSummary(data) {
  const w = data.metrics.outage_event_wait_ms ? data.metrics.outage_event_wait_ms.values : null;
  const report = buildReport(data, 'Consumer outage scenario',
    [
      `Consumer stopped for ${__ENV.OUTAGE_SECONDS || '?'} s; ${expectedCount} transactions were committed during the outage.`,
      `Messages waiting in the queue right before the restart: ${__ENV.QUEUE_DEPTH || '?'}.`,
      'After the restart the consumer drained the queue.',
    ],
    [
      { label: 'Transactions created while consumer was down', metric: 'events_expected', expected: expectedCount },
      { label: 'Events processed exactly once', metric: 'events_processed_exactly_once', expected: expectedCount },
      { label: 'Events missing (lost)', metric: 'events_missing', expected: 0 },
      { label: 'Events whose effect was applied more than once', metric: 'events_with_duplicated_effect', expected: 0 },
    ],
    w ? [`Each event waited ${(w.min / 1000).toFixed(1)}-${(w.max / 1000).toFixed(1)} s between its commit and its processing, i.e. RabbitMQ kept it during the outage.`] : []);
  return reportFiles(REPORT_DIR, 'consumer-outage', report, data);
}
