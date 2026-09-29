ALTER TABLE payments
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_idempotency_key
        UNIQUE (idempotency_key);