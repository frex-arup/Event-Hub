-- Add retry_count column for payment reconciliation retry tracking
ALTER TABLE payments ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;
