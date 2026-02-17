package com.eventhub.booking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Generates QR code images as Base64-encoded PNG data URIs.
 * The QR payload contains a JSON string with booking verification data.
 */
@Service
@Slf4j
public class QrCodeService {

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;

    /**
     * Generate a Base64-encoded PNG QR code for a booking.
     *
     * @param bookingId The booking UUID
     * @param eventId   The event UUID
     * @param userId    The user UUID
     * @param seatCount Number of seats booked
     * @return Base64-encoded data URI string (data:image/png;base64,...)
     */
    public String generateBookingQrCode(UUID bookingId, UUID eventId, UUID userId, int seatCount) {
        String payload = String.format(
                "{\"bookingId\":\"%s\",\"eventId\":\"%s\",\"userId\":\"%s\",\"seats\":%d,\"ts\":%d}",
                bookingId, eventId, userId, seatCount, System.currentTimeMillis()
        );

        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 2
            );

            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            byte[] pngBytes = outputStream.toByteArray();

            String base64 = Base64.getEncoder().encodeToString(pngBytes);
            return "data:image/png;base64," + base64;

        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for booking {}: {}", bookingId, e.getMessage());
            // Fallback to text-based QR identifier
            return "QR:" + bookingId + ":" + eventId + ":" + userId;
        }
    }
}
