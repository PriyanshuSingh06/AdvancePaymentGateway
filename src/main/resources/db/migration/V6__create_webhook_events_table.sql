CREATE TABLE webhook_events (
                                id BIGSERIAL PRIMARY KEY,
                                event_id VARCHAR(255) NOT NULL UNIQUE,
                                payment_reference VARCHAR(255) NOT NULL,
                                received_at TIMESTAMP NOT NULL
);