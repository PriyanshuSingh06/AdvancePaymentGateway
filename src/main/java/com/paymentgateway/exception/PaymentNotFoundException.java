package com.paymentgateway.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Payment not found with id: " + id);
    }

    public PaymentNotFoundException(String paymentReference) {
        super("Payment not found with payment reference: " + paymentReference);
    }
}