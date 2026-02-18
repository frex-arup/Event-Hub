package com.eventhub.payment.service;

import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentGateway;
import com.eventhub.payment.entity.PaymentStatus;
import com.eventhub.payment.gateway.PaymentGatewayFactory;
import com.eventhub.payment.gateway.PaymentGatewayProvider;
import com.eventhub.payment.gateway.PaymentGatewayProvider.*;
import com.eventhub.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGatewayFactory gatewayFactory;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private PaymentGatewayProvider stripeProvider;

    @InjectMocks private PaymentService paymentService;

    @Captor private ArgumentCaptor<Payment> paymentCaptor;

    private UUID bookingId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // ─────────────────────────────────────────────
    // initiatePayment
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePaymentTests {

        @Test
        @DisplayName("should create payment and gateway session")
        void shouldInitiatePayment() {
            when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
            when(gatewayFactory.getProvider(PaymentGateway.STRIPE)).thenReturn(stripeProvider);
            when(stripeProvider.createSession(any(CreateSessionRequest.class)))
                    .thenReturn(new PaymentSessionResult(true, "sess_123", "https://stripe.com/pay", null));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            Payment result = paymentService.initiatePayment(
                    bookingId, userId, BigDecimal.valueOf(100), "USD", "STRIPE",
                    "http://return.url", "idem-1");

            assertThat(result.getGateway()).isEqualTo(PaymentGateway.STRIPE);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(result.getGatewaySessionId()).isEqualTo("sess_123");
            assertThat(result.getRedirectUrl()).isEqualTo("https://stripe.com/pay");
        }

        @Test
        @DisplayName("should return existing payment for duplicate idempotency key")
        void shouldReturnExistingForDuplicateKey() {
            Payment existing = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                    .status(PaymentStatus.PROCESSING).idempotencyKey("idem-1")
                    .build();
            when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

            Payment result = paymentService.initiatePayment(
                    bookingId, userId, BigDecimal.valueOf(100), "USD", "STRIPE",
                    "http://return.url", "idem-1");

            assertThat(result.getId()).isEqualTo(existing.getId());
            verify(gatewayFactory, never()).getProvider(any());
        }

        @Test
        @DisplayName("should throw if gateway session creation fails")
        void shouldThrowIfGatewayFails() {
            when(paymentRepository.findByIdempotencyKey("idem-fail")).thenReturn(Optional.empty());
            when(gatewayFactory.getProvider(PaymentGateway.STRIPE)).thenReturn(stripeProvider);
            when(stripeProvider.createSession(any(CreateSessionRequest.class)))
                    .thenReturn(new PaymentSessionResult(false, null, null, "Card declined"));

            assertThatThrownBy(() -> paymentService.initiatePayment(
                    bookingId, userId, BigDecimal.valueOf(100), "USD", "STRIPE",
                    "http://return.url", "idem-fail"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Card declined");
        }
    }

    // ─────────────────────────────────────────────
    // handleWebhook
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("handleWebhook")
    class HandleWebhookTests {

        @Test
        @DisplayName("should mark payment SUCCESS and publish event")
        void shouldHandleSuccessWebhook() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                    .status(PaymentStatus.PROCESSING).gatewaySessionId("sess_123")
                    .build();
            when(paymentRepository.findByGatewaySessionId("sess_123")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.handleWebhook("STRIPE", "sess_123", "txn_abc", "SUCCESS", BigDecimal.valueOf(100));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getGatewayTransactionId()).isEqualTo("txn_abc");
            verify(kafkaTemplate).send(eq("payment-events"), anyString(), argThat(map -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) map;
                return "payment.success".equals(m.get("eventType"));
            }));
        }

        @Test
        @DisplayName("should mark payment FAILED on failure webhook")
        void shouldHandleFailureWebhook() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                    .status(PaymentStatus.PROCESSING).gatewaySessionId("sess_456")
                    .build();
            when(paymentRepository.findByGatewaySessionId("sess_456")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.handleWebhook("STRIPE", "sess_456", "txn_def", "FAILED", BigDecimal.valueOf(100));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should skip if payment already in terminal state")
        void shouldSkipTerminalState() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                    .status(PaymentStatus.SUCCESS).gatewaySessionId("sess_789")
                    .build();
            when(paymentRepository.findByGatewaySessionId("sess_789")).thenReturn(Optional.of(payment));

            paymentService.handleWebhook("STRIPE", "sess_789", "txn_ghi", "SUCCESS", BigDecimal.valueOf(100));

            verify(paymentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // refundPayment
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("refundPayment")
    class RefundPaymentTests {

        @Test
        @DisplayName("should refund a successful payment")
        void shouldRefundPayment() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                    .status(PaymentStatus.SUCCESS).gateway(PaymentGateway.STRIPE)
                    .gatewayTransactionId("txn_abc")
                    .amount(BigDecimal.valueOf(100)).currency("USD")
                    .build();
            when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));
            when(gatewayFactory.getProvider(PaymentGateway.STRIPE)).thenReturn(stripeProvider);
            when(stripeProvider.refund("txn_abc", BigDecimal.valueOf(100), "USD"))
                    .thenReturn(new RefundResult(true, "ref_123", BigDecimal.valueOf(100), null));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentService.refundPayment(bookingId);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.getRefundId()).isEqualTo("ref_123");
        }

        @Test
        @DisplayName("should throw if payment not successful")
        void shouldThrowIfNotSuccessful() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID()).bookingId(bookingId)
                    .status(PaymentStatus.PROCESSING).build();
            when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.refundPayment(bookingId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only refund successful");
        }
    }
}
