package com.eventhub.identity.controller;

import com.eventhub.identity.dto.AuthRequest;
import com.eventhub.identity.dto.AuthResponse;
import com.eventhub.identity.service.AuthService;
import com.eventhub.identity.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest.Signup request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody AuthRequest.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> me(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    @PostMapping("/oauth/{provider}")
    public ResponseEntity<Map<String, Object>> oauthLogin(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        String code = body.get("code");
        String redirectUri = body.getOrDefault("redirectUri", "http://localhost:3000/auth/callback");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Authorization code is required"));
        }
        return ResponseEntity.ok(oAuthService.authenticateWithOAuth(provider, code, redirectUri));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
