package com.paymentgateway.service;

import com.paymentgateway.entity.Payment;

public interface PaymentProcessor {

    boolean process(Payment payment);
}