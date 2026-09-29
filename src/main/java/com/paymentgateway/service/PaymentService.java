package com.paymentgateway.service;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentAttempt;
import com.paymentgateway.entity.PaymentAttemptStatus;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.exception.InvalidPaymentException;
import com.paymentgateway.exception.PaymentNotFoundException;
import com.paymentgateway.repository.PaymentAttemptRepository;
import com.paymentgateway.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProcessor paymentProcessor;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentProcessor paymentProcessor) {

        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentProcessor = paymentProcessor;
    }

    // CREATE PAYMENT
    public PaymentResponse createPayment(
            PaymentRequest request,
            String idempotencyKey) {

        Payment existingPayment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingPayment != null) {
            return convertToResponse(existingPayment);
        }

        Payment payment = new Payment();

        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setIdempotencyKey(idempotencyKey);

        Payment savedPayment = paymentRepository.save(payment);

        return convertToResponse(savedPayment);
    }

    // GET PAYMENT BY ID
    public PaymentResponse getPaymentById(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        return convertToResponse(payment);
    }

    // GET ALL PAYMENTS
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // PROCESS NEW PAYMENT
    @Transactional
    public PaymentResponse processPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.CREATED) {

            throw new InvalidPaymentException(
                    "Payment cannot be processed. Current status: "
                            + payment.getStatus()
            );
        }

        return executePaymentProcessing(payment);
    }

    // RETRY FAILED PAYMENT
    @Transactional
    public PaymentResponse retryPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.FAILED) {

            throw new InvalidPaymentException(
                    "Payment cannot be retried. Current status: "
                            + payment.getStatus()
            );
        }

        return executePaymentProcessing(payment);
    }

    // COMMON PAYMENT PROCESSING LOGIC
    private PaymentResponse executePaymentProcessing(Payment payment) {

        /*
         * Find how many attempts already exist.
         *
         * Example:
         * 0 existing attempts → attempt number 1
         * 1 existing attempt  → attempt number 2
         * 2 existing attempts → attempt number 3
         */
        Integer existingAttempts =
                paymentAttemptRepository
                        .countByPaymentId(payment.getId());

        int attemptNumber = existingAttempts + 1;

        /*
         * Create PaymentAttempt
         */
        PaymentAttempt attempt = new PaymentAttempt();

        attempt.setPayment(payment);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(PaymentAttemptStatus.CREATED);
        attempt.setCreatedAt(LocalDateTime.now());

        PaymentAttempt savedAttempt =
                paymentAttemptRepository.save(attempt);

        /*
         * Keep both sides of the JPA relationship synchronized.
         */
        payment.getAttempts().add(savedAttempt);

        /*
         * Payment → PENDING
         */
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        /*
         * Attempt → PROCESSING
         */
        savedAttempt.setStatus(PaymentAttemptStatus.PROCESSING);
        paymentAttemptRepository.save(savedAttempt);

        /*
         * Send payment to processor
         */
        PaymentProcessorResult result =
                paymentProcessor.process(payment);

        /*
         * Processor result
         */
        if (result.isSuccessful()) {

            savedAttempt.setStatus(PaymentAttemptStatus.SUCCESS);

            savedAttempt.setProcessorReference(
                    result.getProcessorReference()
            );

            savedAttempt.setCompletedAt(
                    LocalDateTime.now()
            );

            payment.setStatus(PaymentStatus.SUCCESS);

        } else {

            savedAttempt.setStatus(PaymentAttemptStatus.FAILED);

            savedAttempt.setFailureReason(
                    result.getFailureReason()
            );

            savedAttempt.setCompletedAt(
                    LocalDateTime.now()
            );

            payment.setStatus(PaymentStatus.FAILED);
        }

        /*
         * Save final attempt
         */
        paymentAttemptRepository.save(savedAttempt);

        /*
         * Save final payment
         */
        Payment processedPayment =
                paymentRepository.save(payment);

        return convertToResponse(processedPayment);
    }

    // REFUND PAYMENT
    public PaymentResponse refundPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {

            throw new InvalidPaymentException(
                    "Payment cannot be refunded. Current status: "
                            + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        Payment refundedPayment =
                paymentRepository.save(payment);

        return convertToResponse(refundedPayment);
    }

    // ENTITY → RESPONSE DTO
    private PaymentResponse convertToResponse(Payment payment) {

        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setPaymentReference(
                payment.getPaymentReference()
        );
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setCustomerEmail(payment.getCustomerEmail());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        return response;
    }
}