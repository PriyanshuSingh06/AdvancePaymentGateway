package com.paymentgateway.controller;

import com.paymentgateway.dto.PaymentWebhookRequest;
import com.paymentgateway.service.WebhookService;
import com.paymentgateway.service.WebhookSignatureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookSignatureService webhookSignatureService;

    public WebhookController(
            WebhookService webhookService,
            WebhookSignatureService webhookSignatureService) {

        this.webhookService = webhookService;
        this.webhookSignatureService = webhookSignatureService;
    }

    @PostMapping("/payment")
    public ResponseEntity<String> handlePaymentWebhook(
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestBody String rawPayload) {

        // 1. Verify webhook signature
        boolean valid = webhookSignatureService.verifySignature(
                rawPayload,
                signature
        );

        if (!valid) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook signature");
        }

        // 2. Convert JSON payload to PaymentWebhookRequest
        webhookService.handlePaymentWebhook(rawPayload);

        return ResponseEntity.ok("Webhook processed successfully");
    }
}