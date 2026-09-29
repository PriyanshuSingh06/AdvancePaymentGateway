package com.paymentgateway.controller;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {

        return paymentService.getPaymentById(id);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {

        return paymentService.getAllPayments();
    }

    @PostMapping("/{id}/process")
    public PaymentResponse processPayment(@PathVariable Long id) {

        return paymentService.processPayment(id);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable Long id) {

        return paymentService.refundPayment(id);
    }
    @PostMapping("/{id}/retry")
    public PaymentResponse retryPayment(@PathVariable Long id) {
        return paymentService.retryPayment(id);
    }
}