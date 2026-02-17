-- Payment Service Schema

CREATE TABLE IF NOT EXISTS payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id              UUID NOT NULL,
    user_id                 UUID NOT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'USD',
    gateway                 VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    gateway_transaction_id  VARCHAR(255),
    gateway_session_id      VARCHAR(255),
    idempotency_key         VARCHAR(255) NOT NULL UNIQUE,
    redirect_url            VARCHAR(1024),
    webhook_payload         JSONB,
    failure_reason          TEXT,
    refund_amount           DECIMAL(12,2),
    refund_id               VARCHAR(255),
    refunded_at             TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payment_audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id  UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    action      VARCHAR(50) NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20),
    details     JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_txn ON payments(gateway_transaction_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payment_audit_payment ON payment_audit_log(payment_id);
