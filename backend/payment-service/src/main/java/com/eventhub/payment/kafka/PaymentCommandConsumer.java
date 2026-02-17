package com.eventhub.payment.kafka;

import com.eventhub.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes payment commands from the booking service saga.
 * Handles: payment.initiate, payment.cancel, payment.refund
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "payment-commands", groupId = "payment-service-group")
    public void handlePaymentCommand(Map<String, Object> command) {
        String commandType = (String) command.get("commandType");
        if (commandType == null) {
            log.warn("Received payment command with no type: {}", command);
            return;
        }

        try {
            switch (commandType) {
                case "payment.initiate" -> handleInitiate(command);
                case "payment.cancel" -> handleCancel(command);
                case "payment.refund" -> handleRefund(command);
                default -> log.debug("Ignoring unknown payment command: {}", commandType);
            }
        } catch (Exception e) {
            log.error("Error processing payment command {}: {}", commandType, e.getMessage(), e);
            // In production: send to DLQ
        }
    }

    private void handleInitiate(Map<String, Object> command) {
        UUID bookingId = UUID.fromString((String) command.get("bookingId"));
        UUID userId = UUID.fromString((String) command.get("userId"));
        BigDecimal amount = new BigDecimal((String) command.get("amount"));
        String currency = (String) command.getOrDefault("currency", "USD");
        String gateway = (String) command.getOrDefault("gateway", "STRIPE");
        String returnUrl = (String) command.getOrDefault("returnUrl", "");
        String idempotencyKey = (String) command.get("idempotencyKey");

        log.info("Processing payment.initiate: booking={} amount={} {} gateway={}",
                bookingId, amount, currency, gateway);

        paymentService.initiatePayment(bookingId, userId, amount, currency, gateway, returnUrl, idempotencyKey);
    }

    private void handleCancel(Map<String, Object> command) {
        String bookingIdStr = (String) command.get("bookingId");
        log.info("Processing payment.cancel: booking={}", bookingIdStr);
        // Cancel is a no-op if payment hasn't completed yet
        // The payment will simply expire at the gateway level
    }

    private void handleRefund(Map<String, Object> command) {
        UUID bookingId = UUID.fromString((String) command.get("bookingId"));
        log.info("Processing payment.refund: booking={}", bookingId);

        try {
            paymentService.refundPayment(bookingId);
        } catch (Exception e) {
            log.error("Refund failed for booking {}: {}", bookingId, e.getMessage());
        }
    }
}
