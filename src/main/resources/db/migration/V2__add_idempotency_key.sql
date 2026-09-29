ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(255);