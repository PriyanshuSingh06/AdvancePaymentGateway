package com.paymentgateway.state;

import com.paymentgateway.enums.PaymentStatus;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS =
            Map.of(
                    PaymentStatus.CREATED,
                    Set.of(PaymentStatus.PROCESSING),

                    PaymentStatus.PROCESSING,
                    Set.of(
                            PaymentStatus.SUCCESS,
                            PaymentStatus.FAILED
                    ),

                    PaymentStatus.FAILED,
                    Set.of(PaymentStatus.PROCESSING),

                    PaymentStatus.SUCCESS,
                    Set.of(PaymentStatus.REFUNDING),

                    PaymentStatus.REFUNDING,
                    Set.of(PaymentStatus.REFUNDED),

                    PaymentStatus.REFUNDED,
                    Set.of()
            );

    public boolean isValidTransition(
            PaymentStatus current,
            PaymentStatus next) {

        return VALID_TRANSITIONS
                .getOrDefault(current, Set.of())
                .contains(next);
    }
}