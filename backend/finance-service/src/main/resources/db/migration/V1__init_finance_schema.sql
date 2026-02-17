-- Finance Service Schema

CREATE TABLE IF NOT EXISTS budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    organizer_id UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    total_budget DECIMAL(12,2) NOT NULL DEFAULT 0,
    spent       DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS budget_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id       UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    category        VARCHAR(100) NOT NULL,
    description     TEXT,
    estimated_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    actual_amount   DECIMAL(10,2),
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS revenue_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    organizer_id UUID NOT NULL,
    booking_id  UUID,
    amount      DECIMAL(12,2) NOT NULL,
    currency    VARCHAR(3) NOT NULL DEFAULT 'USD',
    type        VARCHAR(30) NOT NULL DEFAULT 'TICKET_SALE',
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id    UUID NOT NULL,
    event_id        UUID,
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payout_method   VARCHAR(50),
    payout_ref      VARCHAR(255),
    settled_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_budgets_event ON budgets(event_id);
CREATE INDEX idx_budgets_organizer ON budgets(organizer_id);
CREATE INDEX idx_budget_items_budget ON budget_items(budget_id);
CREATE INDEX idx_revenue_event ON revenue_records(event_id);
CREATE INDEX idx_revenue_organizer ON revenue_records(organizer_id);
CREATE INDEX idx_settlements_organizer ON settlements(organizer_id);
CREATE INDEX idx_settlements_status ON settlements(status);
