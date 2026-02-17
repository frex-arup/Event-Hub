package com.eventhub.payment.gateway;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.RefundRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * PayPal payment gateway implementation using the official PayPal Checkout Server SDK.
 */
@Component
@Slf4j
public class PaypalGatewayProvider implements PaymentGatewayProvider {

    @Value("${payment.gateways.paypal.client-id:}")
    private String clientId;

    @Value("${payment.gateways.paypal.client-secret:}")
    private String clientSecret;

    @Value("${payment.gateways.paypal.mode:sandbox}")
    private String mode;

    private PayPalHttpClient paypalClient;

    @PostConstruct
    void init() {
        if (clientId.isBlank() || clientSecret.isBlank()
                || "placeholder".equals(clientId)) {
            log.warn("PayPal credentials not configured — SDK will not be initialised");
            return;
        }

        PayPalEnvironment environment = "live".equalsIgnoreCase(mode)
                ? new PayPalEnvironment.Live(clientId, clientSecret)
                : new PayPalEnvironment.Sandbox(clientId, clientSecret);

        paypalClient = new PayPalHttpClient(environment);
        log.info("PayPal SDK initialised in {} mode", mode);
    }

    @Override
    public PaymentSessionResult createSession(CreateSessionRequest request) {
        try {
            log.info("Creating PayPal order for booking={} amount={} {}",
                    request.bookingId(), request.amount(), request.currency());

            if (paypalClient == null) {
                log.warn("PayPal SDK not initialised — credentials missing");
                return PaymentSessionResult.failure("PayPal not configured");
            }

            OrdersCreateRequest orderRequest = new OrdersCreateRequest();
            orderRequest.prefer("return=representation");
            orderRequest.requestBody(buildOrderRequest(request));

            HttpResponse<com.paypal.orders.Order> response = paypalClient.execute(orderRequest);
            com.paypal.orders.Order order = response.result();

            String approveUrl = order.links().stream()
                    .filter(link -> "approve".equals(link.rel()))
                    .map(LinkDescription::href)
                    .findFirst()
                    .orElse("");

            log.info("PayPal order created: {}", order.id());
            return PaymentSessionResult.success(order.id(), approveUrl);

        } catch (IOException e) {
            log.error("PayPal order creation failed: {}", e.getMessage(), e);
            return PaymentSessionResult.failure("PayPal error: " + e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(String gatewayTransactionId) {
        try {
            log.info("Verifying PayPal payment (capturing order): {}", gatewayTransactionId);

            if (paypalClient == null) {
                return PaymentVerificationResult.failure("PayPal not configured");
            }

            // First capture the order (PayPal requires explicit capture after approval)
            OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(gatewayTransactionId);
            captureRequest.requestBody(new OrderRequest());

            HttpResponse<com.paypal.orders.Order> response = paypalClient.execute(captureRequest);
            com.paypal.orders.Order order = response.result();

            if ("COMPLETED".equals(order.status())) {
                // Extract the capture ID and amount from the first purchase unit
                PurchaseUnit unit = order.purchaseUnits().get(0);
                Capture capture = unit.payments().captures().get(0);
                BigDecimal amount = new BigDecimal(capture.amount().value());
                String currency = capture.amount().currencyCode();

                return PaymentVerificationResult.success(
                        capture.id(), amount, currency);
            }

            return PaymentVerificationResult.failure("Order status: " + order.status());

        } catch (IOException e) {
            log.error("PayPal verification/capture failed: {}", e.getMessage(), e);
            return PaymentVerificationResult.failure("Verification failed: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(String captureId, BigDecimal amount, String currency) {
        try {
            log.info("Initiating PayPal refund for capture={} amount={} {}",
                    captureId, amount, currency);

            if (paypalClient == null) {
                return RefundResult.failure("PayPal not configured");
            }

            CapturesRefundRequest refundRequest = new CapturesRefundRequest(captureId);
            RefundRequest body = new RefundRequest();
            com.paypal.payments.Money money = new com.paypal.payments.Money();
            money.currencyCode(currency);
            money.value(amount.toPlainString());
            body.amount(money);
            refundRequest.requestBody(body);

            HttpResponse<com.paypal.payments.Refund> response = paypalClient.execute(refundRequest);
            com.paypal.payments.Refund refund = response.result();

            BigDecimal refundedAmount = new BigDecimal(refund.amount().value());
            log.info("PayPal refund created: {}", refund.id());
            return RefundResult.success(refund.id(), refundedAmount);

        } catch (IOException e) {
            log.error("PayPal refund failed: {}", e.getMessage(), e);
            return RefundResult.failure("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public String getGatewayName() {
        return "PAYPAL";
    }

    private OrderRequest buildOrderRequest(CreateSessionRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(request.bookingId())
                .description(request.description() != null
                        ? request.description()
                        : "EventHub Booking " + request.bookingId())
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(request.currency().toUpperCase())
                        .value(request.amount().toPlainString()));

        orderRequest.purchaseUnits(List.of(purchaseUnit));

        ApplicationContext appContext = new ApplicationContext()
                .returnUrl(request.returnUrl() + "?status=success")
                .cancelUrl(request.returnUrl() + "?status=cancelled")
                .userAction("PAY_NOW");
        orderRequest.applicationContext(appContext);

        return orderRequest;
    }
}
