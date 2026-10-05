// Outage scenario, step 1 (consumer is stopped): create a mix of transactions through wallet-service
// and save their ids so the verification step knows exactly what must eventually be processed.
// Driven by scripts/consumer-outage.sh.
import { createUser, deposit, REPORT_DIR, transfer, withdraw } from './lib/api.js';

export const options = { vus: 1, iterations: 1 };

export function setup() {
  const alice = createUser('outage-alice');
  const bob = createUser('outage-bob');
  const transactions = [deposit(alice, 50000)];
  for (let i = 1; i <= 3; i++) {
    transactions.push(deposit(bob, 1000 * i));
    transactions.push(withdraw(alice, 500 * i));
    transactions.push(transfer(alice, bob.walletId, 2000 * i));
  }
  return { createdAt: new Date().toISOString(), transactions };
}

export default function () {}

export function handleSummary(data) {
  const produced = data.setup_data;
  const lines = produced.transactions.map((tx) => `  ${tx.type.padEnd(10)} ${tx.transactionId} trace=${tx.traceId}`);
  return {
    [`${REPORT_DIR}/outage-transactions.json`]: JSON.stringify(produced, null, 2),
    stdout: `\nCreated ${produced.transactions.length} transactions while the consumer was down:\n${lines.join('\n')}\n`,
  };
}
