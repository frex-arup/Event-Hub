package com.eventhub.payment.controller;

import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.service.PaymentService;
import com.eventhub.payment.service.WebhookVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final WebhookVerificationService webhookVerificationService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-User-Id") String userId) {

        Payment payment = paymentService.initiatePayment(
                request.getBookingId(),
                UUID.fromString(userId),
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                request.getGateway() != null ? request.getGateway() : "STRIPE",
                request.getReturnUrl() != null ? request.getReturnUrl() : "",
                request.getIdempotencyKey()
        );

        return ResponseEntity.ok(Map.of(
                "sessionId", payment.getGatewaySessionId() != null ? payment.getGatewaySessionId() : "",
                "redirectUrl", payment.getRedirectUrl() != null ? payment.getRedirectUrl() : "",
                "gateway", payment.getGateway().name(),
                "paymentId", payment.getId().toString()
        ));
    }

    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String gateway,
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature) {

        // Verify webhook signature
        String signature = stripeSignature != null ? stripeSignature : razorpaySignature;
        if (signature != null && !webhookVerificationService.verify(gateway, rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Parse payload after verification
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(rawPayload, Map.class);
        // Fallback: try parsing as JSON
        try {
            payload = new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawPayload, Map.class);
        } catch (Exception ignored) {}

        String sessionId = (String) payload.get("sessionId");
        String transactionId = (String) payload.get("transactionId");
        String status = (String) payload.get("status");
        BigDecimal amount = payload.get("amount") != null
                ? new BigDecimal(payload.get("amount").toString()) : BigDecimal.ZERO;

        paymentService.handleWebhook(gateway, sessionId, transactionId, status, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    @PostMapping("/{bookingId}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.refundPayment(bookingId));
    }

    // ─────────────────────────────────────────────
    // Request DTOs
    // ─────────────────────────────────────────────

    @Data
    public static class InitiatePaymentRequest {
        @NotNull private UUID bookingId;
        @NotNull private BigDecimal amount;
        private String currency;
        private String gateway;
        private String returnUrl;
        @NotNull private String idempotencyKey;
    }

    // ─────────────────────────────────────────────
    // Exception handlers
    // ─────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409, "message", e.getMessage(), "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "message", e.getMessage(), "timestamp", Instant.now().toString()
        ));
    }
}
