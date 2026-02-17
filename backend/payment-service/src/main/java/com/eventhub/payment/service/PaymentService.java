package com.eventhub.payment.service;

import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentGateway;
import com.eventhub.payment.entity.PaymentStatus;
import com.eventhub.payment.gateway.PaymentGatewayFactory;
import com.eventhub.payment.gateway.PaymentGatewayProvider;
import com.eventhub.payment.gateway.PaymentGatewayProvider.*;
import com.eventhub.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Initiate a payment — idempotent via idempotency_key.
     */
    @Transactional
    public Payment initiatePayment(UUID bookingId, UUID userId, BigDecimal amount,
                                   String currency, String gateway, String returnUrl,
                                   String idempotencyKey) {
        // Idempotency: return existing payment if same key
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    PaymentGateway gw = PaymentGateway.valueOf(gateway.toUpperCase());
                    PaymentGatewayProvider provider = gatewayFactory.getProvider(gw);

                    // Create gateway session
                    PaymentSessionResult session = provider.createSession(new CreateSessionRequest(
                            bookingId.toString(), userId.toString(), amount, currency,
                            returnUrl, idempotencyKey,
                            "EventHub Booking " + bookingId
                    ));

                    if (!session.success()) {
                        log.error("Gateway session creation failed: {}", session.errorMessage());
                        publishPaymentEvent("payment.failed", bookingId, null, session.errorMessage());
                        throw new IllegalStateException("Payment initiation failed: " + session.errorMessage());
                    }

                    Payment payment = Payment.builder()
                            .bookingId(bookingId)
                            .userId(userId)
                            .amount(amount)
                            .currency(currency)
                            .gateway(gw)
                            .status(PaymentStatus.PROCESSING)
                            .idempotencyKey(idempotencyKey)
                            .gatewaySessionId(session.sessionId())
                            .redirectUrl(session.redirectUrl())
                            .build();

                    payment = paymentRepository.save(payment);
                    log.info("Payment initiated: id={} booking={} gateway={} session={}",
                            payment.getId(), bookingId, gateway, session.sessionId());

                    return payment;
                });
    }

    /**
     * Handle webhook callback from payment gateway.
     */
    @Transactional
    public void handleWebhook(String gatewayName, String sessionId, String transactionId,
                              String status, BigDecimal amount) {
        Payment payment = paymentRepository.findByGatewaySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for session: " + sessionId));

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Payment {} already in terminal state: {}", payment.getId(), payment.getStatus());
            return;
        }

        payment.setGatewayTransactionId(transactionId);

        if ("SUCCESS".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            log.info("Payment SUCCESS: id={} booking={} txn={}",
                    payment.getId(), payment.getBookingId(), transactionId);

            publishPaymentEvent("payment.success", payment.getBookingId(), payment.getId(), null);

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Gateway returned status: " + status);
            paymentRepository.save(payment);

            log.warn("Payment FAILED: id={} booking={} status={}",
                    payment.getId(), payment.getBookingId(), status);

            publishPaymentEvent("payment.failed", payment.getBookingId(), payment.getId(),
                    "Payment failed with status: " + status);
        }
    }

    /**
     * Process a refund for a completed payment.
     */
    @Transactional
    public Payment refundPayment(UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for booking: " + bookingId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments");
        }

        PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getGateway());
        RefundResult result = provider.refund(
                payment.getGatewayTransactionId(), payment.getAmount(), payment.getCurrency()
        );

        if (result.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundId(result.refundId());
            payment.setRefundAmount(result.refundedAmount());
            payment.setRefundedAt(Instant.now());
            paymentRepository.save(payment);

            log.info("Payment refunded: id={} booking={} refundId={}",
                    payment.getId(), bookingId, result.refundId());

            publishPaymentEvent("payment.refunded", bookingId, payment.getId(), null);
        } else {
            log.error("Refund failed for payment {}: {}", payment.getId(), result.errorMessage());
            throw new IllegalStateException("Refund failed: " + result.errorMessage());
        }

        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBooking(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for booking: " + bookingId));
    }

    private void publishPaymentEvent(String eventType, UUID bookingId, UUID paymentId, String reason) {
        try {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", eventType);
            event.put("bookingId", bookingId.toString());
            event.put("paymentId", paymentId != null ? paymentId.toString() : "");
            event.put("timestamp", Instant.now().toString());
            if (reason != null) event.put("reason", reason);

            kafkaTemplate.send("payment-events", bookingId.toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish payment event: {}", e.getMessage());
        }
    }
}
