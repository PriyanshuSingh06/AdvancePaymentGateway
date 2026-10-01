package com.paymentgateway.kafka;

import com.paymentgateway.event.PaymentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentEvent(PaymentEvent event) {

        System.out.println(
                "KAFKA EVENT BEING SENT: "
                        + event.getPaymentId()
                        + " | "
                        + event.getFromStatus()
                        + " -> "
                        + event.getToStatus()
        );

        kafkaTemplate.send(
                TOPIC,
                event.getPaymentId().toString(),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {

                System.out.println(
                        "KAFKA SEND FAILED: "
                                + exception.getMessage()
                );

            } else {

                System.out.println(
                        "KAFKA SEND SUCCESS: topic="
                                + result.getRecordMetadata().topic()
                                + ", partition="
                                + result.getRecordMetadata().partition()
                                + ", offset="
                                + result.getRecordMetadata().offset()
                );
            }
        });
    }
}