package com.paymentgateway.service;

import com.paymentgateway.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean process(Payment payment) {

        if (payment.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            return false;
        }

        return true;
    }
}