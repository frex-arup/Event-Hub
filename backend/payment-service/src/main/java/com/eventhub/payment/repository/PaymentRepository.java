package com.eventhub.payment.repository;

import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByGatewaySessionId(String sessionId);
    List<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);

    List<Payment> findByStatus(PaymentStatus status);
}
