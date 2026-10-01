CREATE TABLE payment_transactions (
                                      id BIGSERIAL PRIMARY KEY,

                                      payment_id BIGINT NOT NULL,

                                      from_status VARCHAR(50) NOT NULL,

                                      to_status VARCHAR(50) NOT NULL,

                                      created_at TIMESTAMP NOT NULL,

                                      description VARCHAR(500),

                                      CONSTRAINT fk_payment_transactions_payment
                                          FOREIGN KEY (payment_id)
                                              REFERENCES payments(id)
);
