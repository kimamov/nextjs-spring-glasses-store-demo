-- REVIEW: Do not edit V7: existing databases have already recorded its checksum.
ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN ('PENDING_PAYMENT', 'PAID'));
ALTER TABLE orders
    ADD COLUMN stripe_session_id VARCHAR(255) UNIQUE,
    ADD COLUMN stripe_attempt INTEGER NOT NULL DEFAULT 0 CHECK (stripe_attempt >= 0),
    ADD COLUMN paid_at TIMESTAMP WITH TIME ZONE;

-- REVIEW: An expired Stripe session can be replaced with a new attempt. The
-- attempt number gives each replacement a fresh, but retry-stable idempotency key.
