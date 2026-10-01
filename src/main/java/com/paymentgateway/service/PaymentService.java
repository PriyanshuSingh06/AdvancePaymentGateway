package com.paymentgateway.service;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentAttempt;
import com.paymentgateway.entity.PaymentAttemptStatus;
import com.paymentgateway.entity.PaymentTransaction;
import com.paymentgateway.event.PaymentEvent;
import com.paymentgateway.exception.InvalidPaymentException;
import com.paymentgateway.exception.PaymentNotFoundException;
import com.paymentgateway.kafka.PaymentEventProducer;
import com.paymentgateway.repository.PaymentAttemptRepository;
import com.paymentgateway.repository.PaymentRepository;
import com.paymentgateway.repository.PaymentTransactionRepository;
import com.paymentgateway.state.PaymentStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.paymentgateway.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import com.paymentgateway.dto.PaymentTransactionResponse;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProcessor paymentProcessor;
    private final PaymentStateMachine paymentStateMachine;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentProcessor paymentProcessor,
            PaymentStateMachine paymentStateMachine,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentEventProducer paymentEventProducer){

        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentProcessor = paymentProcessor;
        this.paymentStateMachine = paymentStateMachine;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    // =========================================================
    // CREATE PAYMENT
    // =========================================================

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

        Payment savedPayment =
                paymentRepository.save(payment);

        return convertToResponse(savedPayment);
    }

    // =========================================================
    // GET PAYMENT BY ID
    // =========================================================

    public PaymentResponse getPaymentById(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(id));

        return convertToResponse(payment);
    }

    // =========================================================
    // GET ALL PAYMENTS
    // =========================================================

    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // =========================================================
    // PROCESS NEW PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponse processPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(id));

        // CREATED → PROCESSING
        validateTransition(
                payment.getStatus(),
                PaymentStatus.PROCESSING
        );

        return executePaymentProcessing(payment);
    }

    // =========================================================
    // RETRY FAILED PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponse retryPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(id));

        // FAILED → PROCESSING
        validateTransition(
                payment.getStatus(),
                PaymentStatus.PROCESSING
        );

        return executePaymentProcessing(payment);
    }

    // =========================================================
    // COMMON PAYMENT PROCESSING LOGIC
    // =========================================================

    private PaymentResponse executePaymentProcessing(
            Payment payment) {

        Integer existingAttempts =
                paymentAttemptRepository
                        .countByPaymentId(payment.getId());

        int attemptNumber =
                existingAttempts + 1;

        // Create PaymentAttempt
        PaymentAttempt attempt =
                new PaymentAttempt();

        attempt.setPayment(payment);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(
                PaymentAttemptStatus.CREATED
        );
        attempt.setCreatedAt(
                LocalDateTime.now()
        );

        PaymentAttempt savedAttempt =
                paymentAttemptRepository.save(attempt);

        // Keep both sides of JPA relationship synchronized
        payment.getAttempts().add(savedAttempt);

        // Payment → PROCESSING
        changePaymentStatus(
                payment,
                PaymentStatus.PROCESSING
        );

        paymentRepository.save(payment);

        // Attempt → PROCESSING
        savedAttempt.setStatus(
                PaymentAttemptStatus.PROCESSING
        );

        paymentAttemptRepository.save(savedAttempt);

        // Send payment to processor
        PaymentProcessorResult result =
                paymentProcessor.process(payment);

        // Processor result
        if (result.isSuccessful()) {

            // Attempt → SUCCESS
            savedAttempt.setStatus(
                    PaymentAttemptStatus.SUCCESS
            );

            savedAttempt.setProcessorReference(
                    result.getProcessorReference()
            );

            savedAttempt.setCompletedAt(
                    LocalDateTime.now()
            );

            // Payment → SUCCESS
            changePaymentStatus(
                    payment,
                    PaymentStatus.SUCCESS
            );

        } else {

            // Attempt → FAILED
            savedAttempt.setStatus(
                    PaymentAttemptStatus.FAILED
            );

            savedAttempt.setFailureReason(
                    result.getFailureReason()
            );

            savedAttempt.setCompletedAt(
                    LocalDateTime.now()
            );

            // Payment → FAILED
            changePaymentStatus(
                    payment,
                    PaymentStatus.FAILED
            );
        }

        // Save final attempt
        paymentAttemptRepository.save(savedAttempt);

        // Save final payment
        Payment processedPayment =
                paymentRepository.save(payment);

        return convertToResponse(processedPayment);
    }

    // =========================================================
    // REFUND PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponse refundPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(id));

        /*
         * SUCCESS → REFUNDING
         */
        changePaymentStatus(
                payment,
                PaymentStatus.REFUNDING
        );

        paymentRepository.save(payment);

        /*
         * REFUNDING → REFUNDED
         *
         * For now our refund is simulated,
         * so we immediately complete it.
         */
        changePaymentStatus(
                payment,
                PaymentStatus.REFUNDED
        );

        Payment refundedPayment =
                paymentRepository.save(payment);

        return convertToResponse(refundedPayment);
    }

    // =========================================================
    // VALIDATE PAYMENT TRANSITION
    // =========================================================

    private void validateTransition(
            PaymentStatus currentStatus,
            PaymentStatus newStatus) {

        if (!paymentStateMachine.isValidTransition(
                currentStatus,
                newStatus)) {

            throw new InvalidPaymentException(
                    "Invalid payment status transition from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }
    }

    // =========================================================
    // CHANGE PAYMENT STATUS
    // =========================================================

    private void changePaymentStatus(
            Payment payment,
            PaymentStatus newStatus) {

        PaymentStatus currentStatus = payment.getStatus();

        // Validate state transition
        validateTransition(
                currentStatus,
                newStatus
        );

        // Change payment status
        payment.setStatus(newStatus);

        // Create transaction history
        PaymentTransaction transaction =
                new PaymentTransaction();

        transaction.setPayment(payment);
        transaction.setFromStatus(currentStatus);
        transaction.setToStatus(newStatus);
        transaction.setCreatedAt(LocalDateTime.now());

        transaction.setDescription(
                "Payment status changed from "
                        + currentStatus
                        + " to "
                        + newStatus
        );

        paymentTransactionRepository.save(transaction);
        PaymentEvent event = new PaymentEvent();

        event.setPaymentId(payment.getId());
        event.setPaymentReference(payment.getPaymentReference());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setFromStatus(currentStatus);
        event.setToStatus(newStatus);
        event.setTimestamp(LocalDateTime.now());

        paymentEventProducer.publishPaymentEvent(event);
    }

    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private PaymentResponse convertToResponse(
            Payment payment) {

        PaymentResponse response =
                new PaymentResponse();

        response.setId(payment.getId());

        response.setPaymentReference(
                payment.getPaymentReference()
        );

        response.setAmount(
                payment.getAmount()
        );

        response.setCurrency(
                payment.getCurrency()
        );

        response.setStatus(
                payment.getStatus()
        );

        response.setCustomerEmail(
                payment.getCustomerEmail()
        );

        response.setPaymentMethod(
                payment.getPaymentMethod()
        );

        response.setCreatedAt(
                payment.getCreatedAt()
        );

        response.setUpdatedAt(
                payment.getUpdatedAt()
        );

        return response;
    }
    public List<PaymentTransactionResponse> getPaymentTransactions(Long paymentId) {

        paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: " + paymentId
                        )
                );

        return paymentTransactionRepository
                .findByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream()
                .map(transaction -> {

                    PaymentTransactionResponse response =
                            new PaymentTransactionResponse();

                    response.setId(transaction.getId());
                    response.setPaymentId(
                            transaction.getPayment().getId()
                    );
                    response.setFromStatus(
                            transaction.getFromStatus()
                    );
                    response.setToStatus(
                            transaction.getToStatus()
                    );
                    response.setCreatedAt(
                            transaction.getCreatedAt()
                    );
                    response.setDescription(
                            transaction.getDescription()
                    );

                    return response;
                })
                .toList();
    }
}