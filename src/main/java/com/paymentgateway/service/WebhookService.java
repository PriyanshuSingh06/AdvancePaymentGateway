package com.paymentgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.dto.PaymentWebhookRequest;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentAttempt;
import com.paymentgateway.entity.PaymentAttemptStatus;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.WebhookEvent;
import com.paymentgateway.exception.PaymentNotFoundException;
import com.paymentgateway.repository.PaymentAttemptRepository;
import com.paymentgateway.repository.PaymentRepository;
import com.paymentgateway.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            WebhookEventRepository webhookEventRepository,
            ObjectMapper objectMapper) {

        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handlePaymentWebhook(String rawPayload) {

        // 1. Convert JSON payload into DTO
        PaymentWebhookRequest request;

        try {
            request = objectMapper.readValue(
                    rawPayload,
                    PaymentWebhookRequest.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid webhook payload",
                    e
            );
        }

        // 2. Check whether this webhook was already processed
        if (webhookEventRepository
                .findByEventId(request.getEventId())
                .isPresent()) {

            return;
        }

        // 3. Find the payment
        Payment payment = paymentRepository
                .findByPaymentReference(
                        request.getPaymentReference()
                )
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                request.getPaymentReference()
                        ));

        // 4. Find latest payment attempt
        PaymentAttempt attempt = paymentAttemptRepository
                .findTopByPaymentIdOrderByAttemptNumberDesc(
                        payment.getId()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No payment attempt found for payment: "
                                        + payment.getPaymentReference()
                        ));

        // 5. Update payment attempt and payment
        if (request.getStatus() == PaymentStatus.SUCCESS) {

            attempt.setStatus(PaymentAttemptStatus.SUCCESS);

            attempt.setProcessorReference(
                    request.getProcessorReference()
            );

            attempt.setCompletedAt(
                    LocalDateTime.now()
            );

            payment.setStatus(PaymentStatus.SUCCESS);

        } else if (request.getStatus() == PaymentStatus.FAILED) {

            attempt.setStatus(PaymentAttemptStatus.FAILED);

            attempt.setFailureReason(
                    request.getFailureReason()
            );

            attempt.setCompletedAt(
                    LocalDateTime.now()
            );

            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentAttemptRepository.save(attempt);
        paymentRepository.save(payment);

        // 6. Save webhook event
        WebhookEvent webhookEvent = new WebhookEvent();

        webhookEvent.setEventId(
                request.getEventId()
        );

        webhookEvent.setPaymentReference(
                request.getPaymentReference()
        );

        webhookEvent.setReceivedAt(
                LocalDateTime.now()
        );

        webhookEventRepository.save(webhookEvent);
    }
}