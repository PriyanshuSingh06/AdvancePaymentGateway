package com.paymentgateway.dto;

import com.paymentgateway.entity.PaymentStatus;

public class PaymentWebhookRequest {

    private String eventId;
    private String paymentReference;
    private String processorReference;
    private PaymentStatus status;
    private String failureReason;

    public PaymentWebhookRequest() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public void setProcessorReference(String processorReference) {
        this.processorReference = processorReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}