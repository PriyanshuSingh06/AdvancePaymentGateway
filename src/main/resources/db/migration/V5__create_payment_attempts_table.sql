CREATE TABLE payment_attempts (
                                  id BIGSERIAL PRIMARY KEY,

                                  payment_id BIGINT NOT NULL,

                                  attempt_number INTEGER NOT NULL,

                                  status VARCHAR(20) NOT NULL,

                                  processor_reference VARCHAR(255),

                                  failure_reason VARCHAR(500),

                                  created_at TIMESTAMP NOT NULL,

                                  completed_at TIMESTAMP,

                                  CONSTRAINT fk_payment_attempt
                                      FOREIGN KEY (payment_id)
                                          REFERENCES payments(id)
                                          ON DELETE CASCADE
);