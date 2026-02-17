package com.eventhub.payment.service;

import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentStatus;
import com.eventhub.payment.gateway.PaymentGatewayFactory;
import com.eventhub.payment.gateway.PaymentGatewayProvider;
import com.eventhub.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Scheduled reconciliation job that:
 * 1. Detects stale PROCESSING payments and verifies with the gateway
 * 2. Retries failed payments (up to max retries)
 * 3. Expires abandoned payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRIES = 3;
    private static final int STALE_MINUTES = 15;
    private static final int EXPIRE_MINUTES = 60;

    /**
     * Runs every 5 minutes to reconcile stale payments.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void reconcilePayments() {
        log.info("Payment reconciliation job started");

        List<Payment> processing = paymentRepository.findByStatus(PaymentStatus.PROCESSING);
        int reconciled = 0;
        int expired = 0;
        int retried = 0;

        for (Payment payment : processing) {
            Instant createdAt = payment.getCreatedAt();
            long minutesOld = ChronoUnit.MINUTES.between(createdAt, Instant.now());

            if (minutesOld > EXPIRE_MINUTES) {
                // Expire abandoned payments
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment expired after " + EXPIRE_MINUTES + " minutes");
                paymentRepository.save(payment);
                publishPaymentEvent("payment.failed", payment, "Payment expired");
                expired++;
                log.info("Payment expired: id={} booking={}", payment.getId(), payment.getBookingId());

            } else if (minutesOld > STALE_MINUTES) {
                // Verify with gateway
                try {
                    PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getGateway());
                    String sessionId = payment.getGatewaySessionId();

                    if (sessionId != null) {
                        var result = provider.verifyPayment(sessionId);
                        if (result.verified()) {
                            payment.setStatus(PaymentStatus.SUCCESS);
                            payment.setGatewayTransactionId(result.transactionId());
                            paymentRepository.save(payment);
                            publishPaymentEvent("payment.success", payment, null);
                            reconciled++;
                            log.info("Payment reconciled as SUCCESS: id={}", payment.getId());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Reconciliation check failed for payment {}: {}", payment.getId(), e.getMessage());
                }
            }
        }

        // Retry failed payments with retry count < MAX_RETRIES
        List<Payment> failed = paymentRepository.findByStatus(PaymentStatus.FAILED);
        for (Payment payment : failed) {
            if (payment.getRetryCount() < MAX_RETRIES) {
                long minutesSinceCreation = ChronoUnit.MINUTES.between(payment.getCreatedAt(), Instant.now());
                if (minutesSinceCreation < EXPIRE_MINUTES) {
                    try {
                        PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getGateway());
                        var session = provider.createSession(new PaymentGatewayProvider.CreateSessionRequest(
                                payment.getBookingId().toString(),
                                payment.getUserId().toString(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                payment.getRedirectUrl(),
                                payment.getIdempotencyKey() + "_retry" + (payment.getRetryCount() + 1),
                                "EventHub Booking Retry"
                        ));

                        if (session.success()) {
                            payment.setStatus(PaymentStatus.PROCESSING);
                            payment.setGatewaySessionId(session.sessionId());
                            payment.setRedirectUrl(session.redirectUrl());
                            payment.setRetryCount(payment.getRetryCount() + 1);
                            payment.setFailureReason(null);
                            paymentRepository.save(payment);
                            retried++;
                            log.info("Payment retried: id={} attempt={}", payment.getId(), payment.getRetryCount());
                        }
                    } catch (Exception e) {
                        log.warn("Retry failed for payment {}: {}", payment.getId(), e.getMessage());
                    }
                }
            }
        }

        log.info("Payment reconciliation completed: reconciled={} expired={} retried={}", reconciled, expired, retried);
    }

    private void publishPaymentEvent(String eventType, Payment payment, String reason) {
        try {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", eventType);
            event.put("bookingId", payment.getBookingId().toString());
            event.put("paymentId", payment.getId().toString());
            event.put("timestamp", Instant.now().toString());
            if (reason != null) event.put("reason", reason);
            kafkaTemplate.send("payment-events", payment.getBookingId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish payment event: {}", e.getMessage());
        }
    }
}
