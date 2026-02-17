package com.eventhub.payment.gateway;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Razorpay payment gateway implementation using the official Razorpay Java SDK.
 */
@Component
@Slf4j
public class RazorpayGatewayProvider implements PaymentGatewayProvider {

    @Value("${payment.gateways.razorpay.key-id}")
    private String keyId;

    @Value("${payment.gateways.razorpay.key-secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    void init() {
        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);
            log.info("Razorpay SDK initialised (keyId ending ...{})",
                    keyId.length() > 8 ? keyId.substring(keyId.length() - 4) : "****");
        } catch (RazorpayException e) {
            log.error("Failed to initialise Razorpay client: {}", e.getMessage());
        }
    }

    @Override
    public PaymentSessionResult createSession(CreateSessionRequest request) {
        try {
            log.info("Creating Razorpay order for booking={} amount={} {}",
                    request.bookingId(), request.amount(), request.currency());

            // Razorpay expects amount in smallest currency unit (paise for INR, cents for USD)
            int amountInSmallestUnit = request.amount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInSmallestUnit);
            orderRequest.put("currency", request.currency().toUpperCase());
            orderRequest.put("receipt", request.bookingId());
            orderRequest.put("notes", new JSONObject()
                    .put("bookingId", request.bookingId())
                    .put("userId", request.userId()));

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            // Razorpay uses a client-side checkout; the "redirect" URL carries the order ID
            // The frontend opens Razorpay Checkout with this order_id
            String checkoutUrl = "https://api.razorpay.com/v1/checkout/embedded"
                    + "?order_id=" + orderId + "&key_id=" + keyId;

            log.info("Razorpay order created: {}", orderId);
            return PaymentSessionResult.success(orderId, checkoutUrl);

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            return PaymentSessionResult.failure("Razorpay error: " + e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(String gatewayTransactionId) {
        try {
            log.info("Verifying Razorpay payment: {}", gatewayTransactionId);

            Payment payment = razorpayClient.payments.fetch(gatewayTransactionId);
            String status = payment.get("status");

            if ("captured".equals(status)) {
                int amountPaise = payment.get("amount");
                BigDecimal amount = BigDecimal.valueOf(amountPaise)
                        .divide(BigDecimal.valueOf(100));
                String currency = payment.get("currency");
                return PaymentVerificationResult.success(
                        gatewayTransactionId, amount, currency.toUpperCase());
            }

            return PaymentVerificationResult.failure("Razorpay payment status: " + status);

        } catch (RazorpayException e) {
            log.error("Razorpay verification failed: {}", e.getMessage(), e);
            return PaymentVerificationResult.failure("Verification failed: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(String gatewayTransactionId, BigDecimal amount, String currency) {
        try {
            log.info("Initiating Razorpay refund for txn={} amount={} {}",
                    gatewayTransactionId, amount, currency);

            int amountInSmallestUnit = amount.multiply(BigDecimal.valueOf(100)).intValue();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInSmallestUnit);

            com.razorpay.Refund refund = razorpayClient.payments
                    .refund(gatewayTransactionId, refundRequest);
            String refundId = refund.get("id");

            BigDecimal refundedAmount = BigDecimal.valueOf((int) refund.get("amount"))
                    .divide(BigDecimal.valueOf(100));

            log.info("Razorpay refund created: {}", refundId);
            return RefundResult.success(refundId, refundedAmount);

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage(), e);
            return RefundResult.failure("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public String getGatewayName() {
        return "RAZORPAY";
    }
}
