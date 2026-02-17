#!/bin/bash
# Register Debezium CDC connectors for outbox pattern
# Usage: ./register-connectors.sh [DEBEZIUM_HOST]

DEBEZIUM_HOST=${1:-localhost:8083}

echo "Registering Debezium connectors on $DEBEZIUM_HOST..."

# Booking Service - outbox events
curl -s -X POST "http://$DEBEZIUM_HOST/connectors" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "booking-outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "eventhub",
      "database.password": "eventhub_secret",
      "database.dbname": "booking_db",
      "database.server.name": "booking",
      "topic.prefix": "cdc.booking",
      "table.include.list": "public.bookings,public.booking_saga_log",
      "publication.autocreate.mode": "filtered",
      "slot.name": "booking_slot",
      "plugin.name": "pgoutput",
      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
      "transforms.outbox.table.field.event.id": "id",
      "transforms.outbox.table.field.event.key": "booking_id",
      "transforms.outbox.table.field.event.payload": "payload",
      "transforms.outbox.route.topic.replacement": "booking-events",
      "key.converter": "org.apache.kafka.connect.storage.StringConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": false
    }
  }'

echo ""

# Payment Service - outbox events
curl -s -X POST "http://$DEBEZIUM_HOST/connectors" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payment-outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "eventhub",
      "database.password": "eventhub_secret",
      "database.dbname": "payment_db",
      "database.server.name": "payment",
      "topic.prefix": "cdc.payment",
      "table.include.list": "public.payments,public.payment_audit_log",
      "publication.autocreate.mode": "filtered",
      "slot.name": "payment_slot",
      "plugin.name": "pgoutput",
      "key.converter": "org.apache.kafka.connect.storage.StringConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": false
    }
  }'

echo ""

# Seat Inventory - CDC for real-time seat status changes
curl -s -X POST "http://$DEBEZIUM_HOST/connectors" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "seat-inventory-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "eventhub",
      "database.password": "eventhub_secret",
      "database.dbname": "event_db",
      "database.server.name": "seats",
      "topic.prefix": "cdc.seats",
      "table.include.list": "public.seats",
      "publication.autocreate.mode": "filtered",
      "slot.name": "seat_slot",
      "plugin.name": "pgoutput",
      "key.converter": "org.apache.kafka.connect.storage.StringConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": false
    }
  }'

echo ""
echo "All connectors registered."
curl -s "http://$DEBEZIUM_HOST/connectors" | python3 -m json.tool 2>/dev/null || echo "Done."
