package com.paymentgateway.service;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.exception.InvalidPaymentException;
import com.paymentgateway.exception.PaymentNotFoundException;
import com.paymentgateway.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessor paymentProcessor;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProcessor paymentProcessor) {

        this.paymentRepository = paymentRepository;
        this.paymentProcessor = paymentProcessor;
    }

    // CREATE PAYMENT
    public PaymentResponse createPayment(
            PaymentRequest request,
            String idempotencyKey) {

        // Check if this request was already processed
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

    // PROCESS PAYMENT
    public PaymentResponse processPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidPaymentException(
                    "Payment cannot be processed. Current status: "
                            + payment.getStatus()
            );
        }

        // Move payment to PENDING
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Send payment to processor
        boolean successful = paymentProcessor.process(payment);

        // Set final status
        if (successful) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        Payment processedPayment = paymentRepository.save(payment);

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

        Payment refundedPayment = paymentRepository.save(payment);

        return convertToResponse(refundedPayment);
    }

    // ENTITY → RESPONSE DTO
    private PaymentResponse convertToResponse(Payment payment) {

        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
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