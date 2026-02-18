package com.eventhub.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    @DisplayName("should generate a valid Base64-encoded PNG data URI")
    void shouldGenerateBase64QrCode() {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String result = qrCodeService.generateBookingQrCode(bookingId, eventId, userId, 3);

        assertThat(result).startsWith("data:image/png;base64,");
        assertThat(result.length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("should produce different QR codes for different bookings")
    void shouldProduceDifferentCodes() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String qr1 = qrCodeService.generateBookingQrCode(UUID.randomUUID(), eventId, userId, 1);
        String qr2 = qrCodeService.generateBookingQrCode(UUID.randomUUID(), eventId, userId, 1);

        assertThat(qr1).isNotEqualTo(qr2);
    }

    @Test
    @DisplayName("should handle single seat booking")
    void shouldHandleSingleSeat() {
        String result = qrCodeService.generateBookingQrCode(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);

        assertThat(result).startsWith("data:image/png;base64,");
    }
}
