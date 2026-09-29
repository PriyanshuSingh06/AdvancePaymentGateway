package com.paymentgateway.repository;

import com.paymentgateway.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(
            Long paymentId
    );

    Integer countByPaymentId(Long paymentId);

    Optional<PaymentAttempt> findTopByPaymentIdOrderByAttemptNumberDesc(
            Long paymentId
    );
}