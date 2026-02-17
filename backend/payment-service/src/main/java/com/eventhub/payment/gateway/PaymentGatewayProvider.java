package com.eventhub.payment.gateway;

import java.math.BigDecimal;

/**
 * Strategy interface for payment gateway abstraction.
 * Each gateway (Stripe, Razorpay, PayPal) implements this interface.
 */
public interface PaymentGatewayProvider {

    /**
     * Create a payment session / checkout session with the gateway.
     */
    PaymentSessionResult createSession(CreateSessionRequest request);

    /**
     * Verify a payment after webhook callback.
     */
    PaymentVerificationResult verifyPayment(String gatewayTransactionId);

    /**
     * Initiate a refund for a completed payment.
     */
    RefundResult refund(String gatewayTransactionId, BigDecimal amount, String currency);

    /**
     * Which gateway this provider handles.
     */
    String getGatewayName();

    // ─────────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────────

    record CreateSessionRequest(
            String bookingId,
            String userId,
            BigDecimal amount,
            String currency,
            String returnUrl,
            String idempotencyKey,
            String description
    ) {}

    record PaymentSessionResult(
            boolean success,
            String sessionId,
            String redirectUrl,
            String errorMessage
    ) {
        public static PaymentSessionResult success(String sessionId, String redirectUrl) {
            return new PaymentSessionResult(true, sessionId, redirectUrl, null);
        }
        public static PaymentSessionResult failure(String error) {
            return new PaymentSessionResult(false, null, null, error);
        }
    }

    record PaymentVerificationResult(
            boolean verified,
            String transactionId,
            String status,
            BigDecimal amount,
            String currency,
            String errorMessage
    ) {
        public static PaymentVerificationResult success(String txnId, BigDecimal amount, String currency) {
            return new PaymentVerificationResult(true, txnId, "SUCCESS", amount, currency, null);
        }
        public static PaymentVerificationResult failure(String error) {
            return new PaymentVerificationResult(false, null, "FAILED", null, null, error);
        }
    }

    record RefundResult(
            boolean success,
            String refundId,
            BigDecimal refundedAmount,
            String errorMessage
    ) {
        public static RefundResult success(String refundId, BigDecimal amount) {
            return new RefundResult(true, refundId, amount, null);
        }
        public static RefundResult failure(String error) {
            return new RefundResult(false, null, null, error);
        }
    }
}
