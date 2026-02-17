package com.eventhub.payment.gateway;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe payment gateway implementation using the official Stripe Java SDK.
 */
@Component
@Slf4j
public class StripeGatewayProvider implements PaymentGatewayProvider {

    @Value("${payment.gateways.stripe.api-key}")
    private String apiKey;

    @PostConstruct
    void init() {
        Stripe.apiKey = apiKey;
        log.info("Stripe SDK initialised (key ending ...{})",
                apiKey.length() > 8 ? apiKey.substring(apiKey.length() - 4) : "****");
    }

    @Override
    public PaymentSessionResult createSession(CreateSessionRequest request) {
        try {
            log.info("Creating Stripe checkout session for booking={} amount={} {}",
                    request.bookingId(), request.amount(), request.currency());

            long amountInCents = request.amount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setClientReferenceId(request.bookingId())
                    .setSuccessUrl(request.returnUrl() + "?status=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(request.returnUrl() + "?status=cancelled")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.currency().toLowerCase())
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(request.description() != null
                                                    ? request.description()
                                                    : "EventHub Booking " + request.bookingId())
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("bookingId", request.bookingId())
                    .putMetadata("userId", request.userId())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            Session session = Session.create(params, options);

            log.info("Stripe session created: {}", session.getId());
            return PaymentSessionResult.success(session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Stripe session creation failed: code={} message={}",
                    e.getCode(), e.getMessage(), e);
            return PaymentSessionResult.failure("Stripe error: " + e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(String gatewayTransactionId) {
        try {
            log.info("Verifying Stripe payment: {}", gatewayTransactionId);

            // If it looks like a checkout session ID, retrieve the session first
            if (gatewayTransactionId.startsWith("cs_")) {
                Session session = Session.retrieve(gatewayTransactionId);
                if ("complete".equals(session.getStatus())
                        && "paid".equals(session.getPaymentStatus())) {
                    String piId = session.getPaymentIntent();
                    PaymentIntent pi = PaymentIntent.retrieve(piId);
                    BigDecimal amount = BigDecimal.valueOf(pi.getAmount())
                            .divide(BigDecimal.valueOf(100));
                    return PaymentVerificationResult.success(piId, amount,
                            pi.getCurrency().toUpperCase());
                }
                return PaymentVerificationResult.failure(
                        "Session status: " + session.getStatus()
                                + ", payment: " + session.getPaymentStatus());
            }

            // Otherwise treat as a PaymentIntent ID
            PaymentIntent intent = PaymentIntent.retrieve(gatewayTransactionId);
            if ("succeeded".equals(intent.getStatus())) {
                BigDecimal amount = BigDecimal.valueOf(intent.getAmount())
                        .divide(BigDecimal.valueOf(100));
                return PaymentVerificationResult.success(
                        intent.getId(), amount, intent.getCurrency().toUpperCase());
            }
            return PaymentVerificationResult.failure("PaymentIntent status: " + intent.getStatus());

        } catch (StripeException e) {
            log.error("Stripe verification failed: {}", e.getMessage(), e);
            return PaymentVerificationResult.failure("Verification failed: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(String gatewayTransactionId, BigDecimal amount, String currency) {
        try {
            log.info("Initiating Stripe refund for txn={} amount={} {}",
                    gatewayTransactionId, amount, currency);

            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayTransactionId)
                    .setAmount(amountInCents)
                    .build();

            Refund refund = Refund.create(params);

            log.info("Stripe refund created: {}", refund.getId());
            BigDecimal refundedAmount = BigDecimal.valueOf(refund.getAmount())
                    .divide(BigDecimal.valueOf(100));
            return RefundResult.success(refund.getId(), refundedAmount);

        } catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage(), e);
            return RefundResult.failure("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public String getGatewayName() {
        return "STRIPE";
    }
}
