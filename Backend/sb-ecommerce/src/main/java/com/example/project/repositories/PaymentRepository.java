package com.example.project.repositories;

import com.example.project.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Guards against replaying a payment.
     *
     * The order endpoint accepts a payment gateway id from the client, so without
     * this check the same successful PaymentIntent could be submitted repeatedly to
     * create any number of orders for a single charge.
     */
    boolean existsByPgPaymentId(String pgPaymentId);
}
