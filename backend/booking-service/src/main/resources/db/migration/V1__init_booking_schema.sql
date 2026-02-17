-- Booking Service Schema

CREATE TABLE IF NOT EXISTS bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID NOT NULL,
    user_id             UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    payment_id          UUID,
    qr_code             TEXT,
    lock_id             VARCHAR(255),
    saga_state          VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    failure_reason      TEXT,
    expires_at          TIMESTAMP WITH TIME ZONE,
    confirmed_at        TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS booked_seats (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id         UUID NOT NULL,
    section_name    VARCHAR(100) NOT NULL,
    row_label       VARCHAR(20) NOT NULL,
    seat_number     INT NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD'
);

CREATE TABLE IF NOT EXISTS booking_saga_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    step            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    payload         JSONB,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    user_id     UUID NOT NULL,
    section_id  VARCHAR(100),
    seat_count  INT NOT NULL DEFAULT 1,
    status      VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    notified_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- Indexes
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_event ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_idempotency ON bookings(idempotency_key);
CREATE INDEX idx_bookings_saga_state ON bookings(saga_state);
CREATE INDEX idx_booked_seats_booking ON booked_seats(booking_id);
CREATE INDEX idx_booked_seats_seat ON booked_seats(seat_id);
CREATE INDEX idx_saga_log_booking ON booking_saga_log(booking_id);
CREATE INDEX idx_waitlist_event ON waitlist_entries(event_id, status);
