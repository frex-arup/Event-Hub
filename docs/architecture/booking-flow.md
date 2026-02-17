# Booking Flow — Architecture & Sequence

## Critical Path: Seat Selection → Payment → Ticket Issuance

```
┌──────────┐    ┌───────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Frontend  │───▶│API Gateway│───▶│Seat Inventory│───▶│Booking Svc   │───▶│Payment Svc   │───▶│Notification  │
│ (Next.js) │◀───│(SCG)      │◀───│(Redis Lock)  │◀───│(Saga Orch)   │◀───│(Multi-GW)    │◀───│(Email/Push)  │
└──────────┘    └───────────┘    └─────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
     │                                   │                   │                   │
     │              WebSocket            │      Kafka        │      Kafka        │
     │◀──────────────────────────────────│◀──────────────────│◀──────────────────│
```

## Sequence Diagram (Text)

### Happy Path

1. **User selects seats** on the interactive Konva.js seat map
2. **Frontend → API Gateway → Seat Inventory**: `POST /api/v1/seats/lock`
   - Redis Lua script atomically checks + locks all seats
   - DB updated with LOCKED status + TTL (10 min)
   - WebSocket broadcast: `SEAT_LOCKED` to all connected clients
   - Returns: `lockId`, `seatIds`, `expiresAt`

3. **Frontend → API Gateway → Booking Service**: `POST /api/v1/bookings`
   - Idempotency check via `idempotencyKey` (DB unique constraint)
   - Creates booking record: status=PENDING, sagaState=SEATS_LOCKED
   - Kafka publish: `booking.requested`

4. **Frontend → API Gateway → Booking Service**: `POST /api/v1/bookings/{id}/pay`
   - Saga transitions to PAYMENT_PENDING
   - Kafka command: `payment.initiate` → Payment Service

5. **Payment Service** receives Kafka command:
   - Gateway factory resolves provider (Stripe/Razorpay/PayPal)
   - Creates checkout session via gateway SDK
   - Returns redirect URL to frontend

6. **User completes payment** on gateway's hosted page

7. **Payment Gateway webhook** → `POST /api/v1/payments/webhook/{gateway}`
   - Verifies payment with gateway
   - Updates payment status to SUCCESS
   - Kafka publish: `payment.success`

8. **Booking Service** receives `payment.success`:
   - Saga transitions to PAYMENT_COMPLETED
   - Confirms booking: status=CONFIRMED
   - Generates QR code
   - Kafka command: `seats.confirm` → Seat Inventory
   - Kafka event: `booking.confirmed` → Notification Service

9. **Seat Inventory** receives `seats.confirm`:
   - Updates seat status from LOCKED → BOOKED
   - Releases Redis locks (no longer needed)
   - WebSocket broadcast: `SEAT_BOOKED`

10. **Notification Service** receives `booking.confirmed`:
    - Sends email confirmation with QR code
    - Sends push notification
    - Stores in-app notification

### Failure/Compensation Path

**Payment timeout (10 min):**
- Scheduled job in Booking Service detects expired PAYMENT_PENDING bookings
- Triggers Saga compensation:
  - Kafka command: `seats.release` → Seat Inventory
  - Booking status → CANCELLED
  - Kafka event: `booking.failed` → Notification Service

**Payment failure:**
- Payment Service publishes `payment.failed`
- Booking Service triggers compensation (same as above)

**Seat lock expiry:**
- Scheduled job in Seat Inventory releases expired locks (DB + Redis)
- If booking still PENDING, compensation triggers

## Concurrency Safety

| Mechanism | Purpose |
|-----------|---------|
| Redis Lua scripts | Atomic multi-seat locking, prevents race conditions |
| Redis TTL | Locks auto-expire if service crashes |
| DB @Version | Optimistic locking prevents stale writes |
| Idempotency key | Prevents duplicate bookings on retry |
| Kafka idempotent producer | Exactly-once event publishing |
| Saga state machine | Prevents invalid state transitions |

## Kafka Topics

| Topic | Publisher | Consumer | Purpose |
|-------|-----------|----------|---------|
| `booking-events` | Booking Svc | Analytics, Recommendation | Booking lifecycle events |
| `payment-commands` | Booking Svc | Payment Svc | Saga payment commands |
| `payment-events` | Payment Svc | Booking Svc | Payment results |
| `seat-events` | Seat Inventory | Frontend (via WS), Analytics | Seat status changes |
| `seat-commands` | Booking Svc | Seat Inventory | Saga seat commands |
| `notification-events` | Booking Svc | Notification Svc | User notifications |

## Scaling Strategy

- **Seat Inventory**: 3–20 replicas, Redis handles most read load
- **Booking Service**: 3–15 replicas, stateless with DB + Kafka
- **Payment Service**: 3–10 replicas, gateway-bound
- **Kafka partitions**: 12 for seat/booking topics (parallel processing)
- **Redis**: Cluster mode in production (3 masters + 3 replicas)
