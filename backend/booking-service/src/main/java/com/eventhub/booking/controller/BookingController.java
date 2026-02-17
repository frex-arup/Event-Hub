package com.eventhub.booking.controller;

import com.eventhub.booking.entity.BookedSeat;
import com.eventhub.booking.entity.Booking;
import com.eventhub.booking.repository.BookingRepository;
import com.eventhub.booking.saga.BookingSagaOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingSagaOrchestrator sagaOrchestrator;
    private final BookingRepository bookingRepository;

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("X-User-Id") String userId) {

        Booking booking = Booking.builder()
                .eventId(request.getEventId())
                .userId(UUID.fromString(userId))
                .idempotencyKey(request.getIdempotencyKey())
                .currency("USD")
                .build();

        // Add seats with pricing info
        BigDecimal total = BigDecimal.ZERO;
        if (request.getSeats() != null) {
            for (SeatInfo seatInfo : request.getSeats()) {
                BookedSeat bs = BookedSeat.builder()
                        .seatId(seatInfo.getSeatId())
                        .sectionName(seatInfo.getSectionName() != null ? seatInfo.getSectionName() : "")
                        .rowLabel(seatInfo.getRowLabel() != null ? seatInfo.getRowLabel() : "")
                        .seatNumber(seatInfo.getSeatNumber())
                        .price(seatInfo.getPrice() != null ? seatInfo.getPrice() : BigDecimal.ZERO)
                        .currency("USD")
                        .build();
                booking.addSeat(bs);
                total = total.add(bs.getPrice());
            }
        }
        booking.setTotalAmount(total);

        Booking created = sagaOrchestrator.initiateBooking(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(@PathVariable UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<Page<Booking>> getMyBookings(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(
                UUID.fromString(userId), PageRequest.of(page, size));
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @PathVariable UUID bookingId,
            @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") String userId) {

        sagaOrchestrator.requestPayment(
                bookingId,
                request.getGateway() != null ? request.getGateway() : "STRIPE",
                request.getReturnUrl() != null ? request.getReturnUrl() : ""
        );
        return ResponseEntity.ok(Map.of(
                "status", "PAYMENT_INITIATED",
                "bookingId", bookingId.toString()
        ));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") String userId) {
        sagaOrchestrator.cancelBooking(bookingId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // Request DTOs
    // ─────────────────────────────────────────────

    @Data
    public static class CreateBookingRequest {
        @NotNull private UUID eventId;
        @NotNull private String idempotencyKey;
        @NotEmpty private List<SeatInfo> seats;
    }

    @Data
    public static class SeatInfo {
        @NotNull private UUID seatId;
        private String sectionName;
        private String rowLabel;
        private int seatNumber;
        private BigDecimal price;
    }

    @Data
    public static class PaymentRequest {
        private String gateway;
        private String returnUrl;
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

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403, "message", e.getMessage(), "timestamp", Instant.now().toString()
        ));
    }
}
