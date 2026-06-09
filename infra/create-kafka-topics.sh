#!/usr/bin/env bash
set -euo pipefail

# Create production topics with replication factor 3.
# Run against a cluster that has at least 3 brokers.

BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
TOPIC="${KAFKA_TOPIC_LOAN_APPLICATIONS:-loan.applications}"
REPLICATION_FACTOR="${KAFKA_TOPIC_REPLICATION_FACTOR:-3}"
MIN_ISR="${KAFKA_TOPIC_MIN_INSYNC_REPLICAS:-2}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"

kafka-topics \
  --bootstrap-server "${BOOTSTRAP_SERVERS}" \
  --create \
  --if-not-exists \
  --topic "${TOPIC}" \
  --partitions "${PARTITIONS}" \
  --replication-factor "${REPLICATION_FACTOR}" \
  --config min.insync.replicas="${MIN_ISR}"

echo "Created topic ${TOPIC} (partitions=${PARTITIONS}, replication-factor=${REPLICATION_FACTOR}, min.insync.replicas=${MIN_ISR})"
