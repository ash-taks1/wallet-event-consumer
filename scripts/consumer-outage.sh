#!/usr/bin/env bash
# Section 6 scenario: stop the consumer for >= 30 s, commit transactions meanwhile, restart it and
# prove every event is eventually processed exactly once.
#
# Requires the wallet-service stack (wallet-service/docker-compose.yml) to be running.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTAGE_SECONDS="${OUTAGE_SECONDS:-35}"
WALLET_URL="${WALLET_URL:-http://localhost:8090}"
RABBIT_CONTAINER="${RABBIT_CONTAINER:-wallet-rabbitmq}"
QUEUE="wallet-event-consumer.transactions"
COMPOSE=(docker compose -f "$ROOT/docker-compose.yml")

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
# rabbitmqctl reads the queue directly; the management HTTP API only refreshes its stats every ~5 s.
queue_depth() {
  docker exec "$RABBIT_CONTAINER" rabbitmqctl list_queues -q name messages_ready \
    | awk -v q="$QUEUE" '$1 == q { print $2 }'
}

curl -sf --noproxy '*' "$WALLET_URL/actuator/health/readiness" >/dev/null \
  || { echo "wallet-service is not reachable at $WALLET_URL - start its stack first"; exit 1; }

log "Making sure the consumer is running"
"${COMPOSE[@]}" up -d --wait wallet-event-consumer

log "Stopping the consumer"
"${COMPOSE[@]}" stop wallet-event-consumer
stopped_at=$(date +%s)

log "Creating transactions while the consumer is down"
"$ROOT/scripts/k6.sh" outage-produce.js

# Queue counters lag by a second or two; wait until all produced events are visible (max 10 s).
expected=$(jq '.transactions | length' "$ROOT/reports/outage-transactions.json")
for _ in $(seq 1 20); do
  depth=$(queue_depth)
  (( depth >= expected )) && break
  sleep 0.5
done
log "Events waiting in RabbitMQ: $depth"

remaining=$(( OUTAGE_SECONDS - ($(date +%s) - stopped_at) ))
if (( remaining > 0 )); then
  log "Keeping the consumer down for another ${remaining}s"
  sleep "$remaining"
fi
outage=$(( $(date +%s) - stopped_at ))

log "Restarting the consumer after ${outage}s of downtime"
"${COMPOSE[@]}" up -d --wait wallet-event-consumer

log "Verifying every event was processed exactly once"
"$ROOT/scripts/k6.sh" outage-verify.js -e OUTAGE_SECONDS="$outage" -e QUEUE_DEPTH="$depth"

log "Outage scenario passed. Report: $ROOT/reports/consumer-outage-latest.md"
