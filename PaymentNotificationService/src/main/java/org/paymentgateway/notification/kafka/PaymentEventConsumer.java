package org.paymentgateway.notification.kafka;

import org.paymentgateway.notification.event.PaymentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    @KafkaListener(
            topics = "payment-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent event) {

        System.out.println();
        System.out.println("=================================");
        System.out.println("PAYMENT NOTIFICATION SERVICE RECEIVED");
        System.out.println("=================================");
        System.out.println("Payment ID: " + event.getPaymentId());
        System.out.println("Payment Reference: " + event.getPaymentReference());
        System.out.println("Amount: " + event.getAmount());
        System.out.println("Currency: " + event.getCurrency());
        System.out.println("Status: "
                + event.getFromStatus()
                + " -> "
                + event.getToStatus());
        System.out.println("Timestamp: " + event.getTimestamp());
        System.out.println("=================================");
        System.out.println();
    }
}