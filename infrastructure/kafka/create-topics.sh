#!/bin/bash
# Kafka Topic Initialization Script
# Run against the Kafka broker to create all required topics

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:29092}"
PARTITIONS="${DEFAULT_PARTITIONS:-6}"
REPLICATION="${DEFAULT_REPLICATION:-1}"

echo "Creating EventHub Kafka topics..."
echo "Bootstrap: $BOOTSTRAP_SERVER | Partitions: $PARTITIONS | Replication: $REPLICATION"

create_topic() {
    local topic=$1
    local partitions=${2:-$PARTITIONS}
    local retention=${3:-604800000} # 7 days default

    echo "  Creating topic: $topic (partitions=$partitions, retention=${retention}ms)"
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER \
        --create --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$REPLICATION" \
        --config retention.ms="$retention" \
        --config cleanup.policy=delete
}

create_compacted_topic() {
    local topic=$1
    local partitions=${2:-$PARTITIONS}

    echo "  Creating compacted topic: $topic (partitions=$partitions)"
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER \
        --create --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$REPLICATION" \
        --config cleanup.policy=compact \
        --config min.compaction.lag.ms=60000
}

# ─────────────────────────────────────────────
# User & Identity Events
# ─────────────────────────────────────────────
create_topic "user-events" 3

# ─────────────────────────────────────────────
# Event Management Events
# ─────────────────────────────────────────────
create_topic "event-events" 6

# ─────────────────────────────────────────────
# Seat Inventory Events (high throughput)
# ─────────────────────────────────────────────
create_topic "seat-events" 12
create_topic "seat-commands" 12

# ─────────────────────────────────────────────
# Booking Events (high throughput, critical path)
# ─────────────────────────────────────────────
create_topic "booking-events" 12

# ─────────────────────────────────────────────
# Payment Commands & Events
# ─────────────────────────────────────────────
create_topic "payment-commands" 6
create_topic "payment-events" 6

# ─────────────────────────────────────────────
# Notification Events
# ─────────────────────────────────────────────
create_topic "notification-events" 6

# ─────────────────────────────────────────────
# Social Events
# ─────────────────────────────────────────────
create_topic "social-events" 3

# ─────────────────────────────────────────────
# Recommendation Signals
# ─────────────────────────────────────────────
create_topic "recommendation-signals" 3

# ─────────────────────────────────────────────
# Dead Letter Queues
# ─────────────────────────────────────────────
create_topic "booking-events-dlq" 3 2592000000  # 30 days retention
create_topic "payment-events-dlq" 3 2592000000
create_topic "seat-events-dlq" 3 2592000000

# ─────────────────────────────────────────────
# Debezium CDC Topics (auto-created by connectors, but pre-create for control)
# ─────────────────────────────────────────────
create_topic "_connect-configs" 1
create_topic "_connect-offsets" 6
create_topic "_connect-status" 3

echo ""
echo "All topics created successfully!"
echo ""

# List all topics
echo "Current topics:"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list
