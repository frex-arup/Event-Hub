# EventHub — System Architecture Overview

## Service Boundaries

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| api-gateway | 8080 | — | Routing, JWT validation, rate limiting, circuit breaking |
| identity-service | 8081 | identity_db (5432) | Auth, signup/login, OAuth, JWT, user profiles, follows |
| event-service | 8082 | event_db (5433) | Event CRUD, venues, sessions, search |
| venue-layout-service | 8083 | event_db (5433) | Venue layout JSON storage, templates |
| seat-inventory-service | 8084 | event_db (5433) | Seat availability, Redis locking, WebSocket |
| booking-service | 8085 | booking_db (5434) | Saga orchestration, booking lifecycle, waitlist |
| payment-service | 8086 | payment_db (5435) | Multi-gateway payments, webhooks, refunds |
| notification-service | 8087 | identity_db (5432) | Email/SMS/push/in-app notifications |
| social-service | 8088 | social_db (5436) | Posts, comments, DMs, event discussions |
| recommendation-service | 8089 | — | Interest/social-graph recommendations |
| finance-service | 8090 | finance_db (5437) | Budgeting, expenses, revenue analytics |

## Data Flow

```
                    ┌─────────────────┐
                    │   Next.js UI    │
                    │  (App Router)   │
                    └────────┬────────┘
                             │ HTTPS / WSS
                    ┌────────▼────────┐
                    │   API Gateway   │──── Rate Limit (Redis)
                    │  (Spring Cloud) │──── JWT Validation
                    └────────┬────────┘──── Circuit Breakers
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │   Identity   │ │    Event     │ │    Seat      │
    │   Service    │ │   Service    │ │  Inventory   │
    └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
           │                │                │
           ▼                ▼                ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ PostgreSQL   │ │ PostgreSQL   │ │   Redis      │
    │ identity_db  │ │  event_db    │ │  (Locks)     │
    └──────────────┘ └──────────────┘ └──────────────┘
                             │
                     ┌───────▼───────┐
                     │  Apache Kafka │
                     │   (KRaft)     │
                     └───────┬───────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │   Booking    │ │   Payment    │ │ Notification │
    │   Service    │ │   Service    │ │   Service    │
    └──────────────┘ └──────────────┘ └──────────────┘
```

## Key Design Patterns

### 1. Saga Orchestration (Booking)
The Booking Service acts as the saga orchestrator, driving the booking workflow through Kafka commands and events. Each step is idempotent and compensatable.

### 2. Redis Distributed Locking (Seats)
Atomic Lua scripts in Redis ensure no two users can lock the same seat simultaneously, even under 10K+ concurrent requests. TTL ensures locks auto-release on failure.

### 3. CQRS (Seat Availability)
Write path: PostgreSQL (source of truth)
Read path: Redis cache (30s TTL) for fast availability queries

### 4. Event-Driven Architecture
All inter-service communication uses Kafka topics with:
- Idempotent producers (exactly-once semantics)
- Consumer group isolation per service
- Dead letter queues for failed messages
- Debezium CDC for change data capture

### 5. Payment Gateway Strategy Pattern
`PaymentGatewayProvider` interface with implementations for Stripe, Razorpay, PayPal. Factory resolves the correct provider at runtime.

### 6. Circuit Breaker (Resilience4j)
All inter-service HTTP calls and gateway routes use circuit breakers with configurable thresholds, preventing cascade failures.

## Scalability Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Concurrent booking users | 10,000+ | Redis locks, Kafka partitioning, HPA |
| Seat overselling | 0 | Lua atomic locks + DB optimistic locking |
| API latency (p99) | < 200ms | Redis caching, connection pooling |
| Kafka throughput | 50K msg/s | 12 partitions on critical topics |
| DB connections | 30/service | HikariCP pooling |
| Recovery time | < 30s | Health probes, rolling updates |

## Observability Stack

- **Metrics**: Micrometer → Prometheus → Grafana
- **Tracing**: OpenTelemetry → Jaeger/Zipkin
- **Logging**: Structured JSON → Loki/ELK
- **Alerting**: Prometheus alertmanager → PagerDuty/Slack
