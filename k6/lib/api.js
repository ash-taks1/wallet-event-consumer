// Clients for the wallet-service API (to generate transactions) and this consumer's query API.
import http from 'k6/http';
import { fail } from 'k6';

export const WALLET_URL = __ENV.WALLET_URL || 'http://localhost:8090';
export const CONSUMER_URL = __ENV.CONSUMER_URL || 'http://localhost:8091';
export const REPORT_DIR = __ENV.REPORT_DIR || 'reports';

const PASSWORD = 'k6-scenario-password';

export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function createUser(label) {
  const email = `${label}-${Date.now()}-${Math.floor(Math.random() * 1e9)}@k6.example.com`;
  const json = { headers: { 'Content-Type': 'application/json' } };
  const registered = http.post(`${WALLET_URL}/api/v1/auth/register`,
    JSON.stringify({ fullName: `k6 ${label}`, email, password: PASSWORD }), json);
  if (registered.status !== 201) fail(`registration failed: ${registered.status} ${registered.body}`);
  const login = http.post(`${WALLET_URL}/api/v1/auth/login`, JSON.stringify({ email, password: PASSWORD }), json);
  if (login.status !== 200) fail(`login failed: ${login.status} ${login.body}`);
  return { userId: registered.json('userId'), walletId: registered.json('walletId'), token: login.json('accessToken') };
}

function post(user, path, body) {
  const res = http.post(`${WALLET_URL}/api/v1/wallets/${user.walletId}/${path}`, JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${user.token}`, 'Idempotency-Key': uuid() },
  });
  if (res.status !== 201) fail(`${path} failed: ${res.status} ${res.body}`);
  return { transactionId: res.json('transactionId'), traceId: res.headers['X-Trace-Id'] };
}

export function deposit(user, amount) {
  return { ...post(user, 'deposits', { amount }), type: 'DEPOSIT' };
}

export function withdraw(user, amount) {
  return { ...post(user, 'withdrawals', { amount }), type: 'WITHDRAWAL' };
}

export function transfer(user, toWalletId, amount) {
  return { ...post(user, 'transfers', { toWalletId, amount }), type: 'TRANSFER' };
}

export function processedEvents(transactionId) {
  const res = http.get(`${CONSUMER_URL}/api/v1/processed-events?transactionId=${transactionId}`);
  return res.status === 200 ? res.json() : null;
}
