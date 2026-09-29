package com.paymentgateway.service;

import com.paymentgateway.entity.Payment;

public interface PaymentProcessor {

    PaymentProcessorResult process(Payment payment);
}