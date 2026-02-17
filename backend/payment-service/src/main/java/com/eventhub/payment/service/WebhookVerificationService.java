package com.eventhub.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies webhook signatures from payment gateways to ensure
 * authenticity of incoming webhook requests.
 */
@Service
@Slf4j
public class WebhookVerificationService {

    @Value("${payment.gateways.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${payment.gateways.razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${payment.gateways.paypal.webhook-id:}")
    private String paypalWebhookId;

    /**
     * Verify Stripe webhook signature using HMAC-SHA256.
     * Stripe sends signature in Stripe-Signature header.
     */
    public boolean verifyStripeSignature(String payload, String signatureHeader) {
        if (stripeWebhookSecret.isBlank()) {
            log.warn("Stripe webhook secret not configured — skipping verification");
            return true;
        }

        try {
            // Parse the Stripe-Signature header: t=timestamp,v1=signature
            String timestamp = null;
            String expectedSignature = null;
            for (String part : signatureHeader.split(",")) {
                String[] kv = part.split("=", 2);
                if ("t".equals(kv[0])) timestamp = kv[1];
                if ("v1".equals(kv[0])) expectedSignature = kv[1];
            }

            if (timestamp == null || expectedSignature == null) {
                log.warn("Invalid Stripe-Signature header format");
                return false;
            }

            String signedPayload = timestamp + "." + payload;
            String computedSignature = hmacSha256(stripeWebhookSecret, signedPayload);

            boolean valid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    computedSignature.getBytes(StandardCharsets.UTF_8));

            if (!valid) {
                log.warn("Stripe webhook signature mismatch");
            }
            return valid;

        } catch (Exception e) {
            log.error("Stripe signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify Razorpay webhook signature using HMAC-SHA256.
     */
    public boolean verifyRazorpaySignature(String payload, String signature) {
        if (razorpayWebhookSecret.isBlank()) {
            log.warn("Razorpay webhook secret not configured — skipping verification");
            return true;
        }

        try {
            String computedSignature = hmacSha256(razorpayWebhookSecret, payload);
            boolean valid = MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    computedSignature.getBytes(StandardCharsets.UTF_8));

            if (!valid) {
                log.warn("Razorpay webhook signature mismatch");
            }
            return valid;

        } catch (Exception e) {
            log.error("Razorpay signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify PayPal webhook — simplified check via webhook ID.
     * In production, use PayPal's Verify Webhook Signature API.
     */
    public boolean verifyPaypalWebhook(String webhookId, String transmissionId,
                                        String transmissionTime, String certUrl,
                                        String authAlgo, String transmissionSig,
                                        String payload) {
        if (paypalWebhookId.isBlank()) {
            log.warn("PayPal webhook ID not configured — skipping verification");
            return true;
        }

        // In production: POST to https://api-m.paypal.com/v1/notifications/verify-webhook-signature
        // with the above parameters. For now, verify webhook ID matches.
        if (!paypalWebhookId.equals(webhookId)) {
            log.warn("PayPal webhook ID mismatch: expected={}, got={}", paypalWebhookId, webhookId);
            return false;
        }

        log.debug("PayPal webhook ID verified");
        return true;
    }

    /**
     * Route verification to the correct gateway.
     */
    public boolean verify(String gateway, String payload, String signature) {
        return switch (gateway.toUpperCase()) {
            case "STRIPE" -> verifyStripeSignature(payload, signature);
            case "RAZORPAY" -> verifyRazorpaySignature(payload, signature);
            default -> {
                log.warn("No webhook verification for gateway: {}", gateway);
                yield true;
            }
        };
    }

    private String hmacSha256(String secret, String message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
