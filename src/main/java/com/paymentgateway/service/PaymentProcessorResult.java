package com.paymentgateway.service;

public class PaymentProcessorResult {

    private final boolean successful;
    private final String processorReference;
    private final String failureReason;

    public PaymentProcessorResult(
            boolean successful,
            String processorReference,
            String failureReason) {

        this.successful = successful;
        this.processorReference = processorReference;
        this.failureReason = failureReason;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public String getFailureReason() {
        return failureReason;
    }
}