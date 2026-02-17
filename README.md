# EventHub — Production-Grade Event Management Platform

A distributed, event-driven platform for event management, ticketing, social networking, and event activities. Designed to support 10,000+ concurrent booking users with horizontal scalability to millions.

## Architecture Overview

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────────────────┐
│  Next.js UI  │────▶│  API Gateway  │────▶│  Microservices (Spring Boot)     │
│  (App Router)│◀────│  (SCG/Kong)   │◀────│                                  │
└─────────────┘     └──────────────┘     │  ┌─────────────────────────────┐ │
                                          │  │ identity-service            │ │
                                          │  │ user-profile-service        │ │
                                          │  │ event-service               │ │
                                          │  │ venue-layout-service        │ │
                                          │  │ seat-inventory-service      │ │
                                          │  │ booking-service             │ │
                                          │  │ payment-service             │ │
                                          │  │ notification-service        │ │
                                          │  │ social-service              │ │
                                          │  │ recommendation-service      │ │
                                          │  │ finance-service             │ │
                                          │  └─────────────────────────────┘ │
                                          └──────────────────────────────────┘
                                                        │
                              ┌──────────────────────────┼──────────────────┐
                              ▼                          ▼                  ▼
                        ┌──────────┐            ┌─────────────┐     ┌───────────┐
                        │PostgreSQL│            │ Apache Kafka │     │   Redis    │
                        │ (per svc)│            │  (KRaft)     │     │  Cluster   │
                        └──────────┘            └─────────────┘     └───────────┘
                                                       │
                                                ┌──────┴──────┐
                                                │  Debezium   │
                                                │    CDC      │
                                                └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js 14 (App Router), TypeScript, TailwindCSS, shadcn/ui, TanStack Query, Zustand, Konva.js |
| Backend | Spring Boot 3.2 (Java 21), Spring Cloud Gateway |
| Messaging | Apache Kafka (KRaft), Debezium CDC |
| Database | PostgreSQL 16 (per-service), Redis 7 |
| Search | Elasticsearch 8 |
| Auth | JWT + OAuth2 (Google, Apple) |
| Payments | Stripe, Razorpay, PayPal |
| Observability | OpenTelemetry, Prometheus, Grafana, Loki |
| Deployment | Docker, Kubernetes, Helm |

## Project Structure

```
Event3/
├── frontend/                    # Next.js application
├── backend/
│   ├── api-gateway/
│   ├── identity-service/
│   ├── user-profile-service/
│   ├── event-service/
│   ├── venue-layout-service/
│   ├── seat-inventory-service/
│   ├── booking-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── social-service/
│   ├── recommendation-service/
│   └── finance-service/
├── infrastructure/
│   ├── docker/
│   ├── k8s/
│   └── monitoring/
├── docs/
│   ├── architecture/
│   ├── api-contracts/
│   └── schemas/
└── docker-compose.yml
```

## Key Design Decisions

1. **Saga Orchestration** for booking/payment workflows
2. **Redis Distributed Locks** for seat reservation (survives restarts via persistence)
3. **Idempotent APIs** with idempotency keys for all booking operations
4. **CQRS** for read-heavy seat availability queries
5. **Event Sourcing** via Kafka for audit trail
6. **Circuit Breakers** (Resilience4j) for inter-service calls
7. **Rate Limiting** at API Gateway level

## Getting Started

```bash
# Start infrastructure
docker-compose up -d

# Start frontend
cd frontend && npm install && npm run dev

# Start backend services (each service)
cd backend/<service-name> && ./mvnw spring-boot:run
```

## Critical Booking Flow

```
User selects seats → Lock seats (Redis TTL) → Create booking (PENDING)
→ Initiate payment → Payment callback → Confirm booking → Issue ticket
→ Release lock → Send notifications
```

If payment fails or times out: Saga compensation releases locks and cancels booking.
