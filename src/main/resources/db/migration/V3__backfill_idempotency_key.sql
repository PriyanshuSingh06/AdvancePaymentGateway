UPDATE payments
SET idempotency_key = 'legacy-' || id
WHERE idempotency_key IS NULL;