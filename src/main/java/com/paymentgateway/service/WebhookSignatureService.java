package com.paymentgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class WebhookSignatureService {

    private final String webhookSecret;

    public WebhookSignatureService(
            @Value("${payment.webhook.secret}") String webhookSecret) {

        this.webhookSecret = webhookSecret;
    }

    public String generateSignature(String payload) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to generate webhook signature",
                    e
            );
        }
    }

    public boolean verifySignature(
            String payload,
            String providedSignature) {

        String expectedSignature = generateSignature(payload);

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}