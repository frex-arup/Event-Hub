-- Seat Inventory Schema (shares event_db with event-service)

CREATE TABLE IF NOT EXISTS seats (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    section_id  VARCHAR(100) NOT NULL,
    row_label   VARCHAR(20) NOT NULL,
    seat_number INT NOT NULL,
    label       VARCHAR(20),
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    price       DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'USD',
    x_pos       DOUBLE PRECISION NOT NULL DEFAULT 0,
    y_pos       DOUBLE PRECISION NOT NULL DEFAULT 0,
    locked_by   UUID,
    locked_at   TIMESTAMP WITH TIME ZONE,
    lock_expires_at TIMESTAMP WITH TIME ZONE,
    booked_by   UUID,
    booking_id  UUID,
    version     INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, section_id, row_label, seat_number)
);

CREATE TABLE IF NOT EXISTS seat_locks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL,
    user_id         UUID NOT NULL,
    seat_ids        UUID[] NOT NULL,
    lock_key        VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Critical indexes for high-concurrency seat queries
CREATE INDEX idx_seats_event_status ON seats(event_id, status);
CREATE INDEX idx_seats_event_section ON seats(event_id, section_id);
CREATE INDEX idx_seats_locked_by ON seats(locked_by) WHERE locked_by IS NOT NULL;
CREATE INDEX idx_seats_lock_expires ON seats(lock_expires_at) WHERE lock_expires_at IS NOT NULL;
CREATE INDEX idx_seat_locks_event_user ON seat_locks(event_id, user_id);
CREATE INDEX idx_seat_locks_expires ON seat_locks(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_seat_locks_lock_key ON seat_locks(lock_key);
