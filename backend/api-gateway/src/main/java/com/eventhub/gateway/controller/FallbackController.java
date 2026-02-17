package com.eventhub.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/identity")
    public Mono<ResponseEntity<Map<String, Object>>> identityFallback() {
        return buildFallbackResponse("Identity service is temporarily unavailable");
    }

    @GetMapping("/user-profile")
    public Mono<ResponseEntity<Map<String, Object>>> userProfileFallback() {
        return buildFallbackResponse("User profile service is temporarily unavailable");
    }

    @GetMapping("/event")
    public Mono<ResponseEntity<Map<String, Object>>> eventFallback() {
        return buildFallbackResponse("Event service is temporarily unavailable");
    }

    @GetMapping("/venue")
    public Mono<ResponseEntity<Map<String, Object>>> venueFallback() {
        return buildFallbackResponse("Venue service is temporarily unavailable");
    }

    @GetMapping("/seat")
    public Mono<ResponseEntity<Map<String, Object>>> seatFallback() {
        return buildFallbackResponse("Seat inventory service is temporarily unavailable");
    }

    @GetMapping("/booking")
    public Mono<ResponseEntity<Map<String, Object>>> bookingFallback() {
        return buildFallbackResponse("Booking service is temporarily unavailable. Please try again shortly.");
    }

    @GetMapping("/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return buildFallbackResponse("Payment service is temporarily unavailable. Your booking is safe.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String message) {
        Map<String, Object> body = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message,
                "timestamp", Instant.now().toString()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
