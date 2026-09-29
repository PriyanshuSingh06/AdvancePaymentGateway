package com.paymentgateway.service;

import com.paymentgateway.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessorResult process(Payment payment) {

        // First attempt fails
        if (payment.getAttempts().size() == 1) {

            return new PaymentProcessorResult(
                    false,
                    null,
                    "Payment processor rejected the payment"
            );
        }

        // Retry succeeds
        return new PaymentProcessorResult(
                true,
                "MOCK-TXN-" + UUID.randomUUID(),
                null
        );
    }
}