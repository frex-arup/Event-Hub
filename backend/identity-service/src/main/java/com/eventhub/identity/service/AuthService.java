package com.eventhub.identity.service;

import com.eventhub.identity.dto.AuthRequest;
import com.eventhub.identity.dto.AuthResponse;
import com.eventhub.identity.entity.RefreshToken;
import com.eventhub.identity.entity.User;
import com.eventhub.identity.entity.UserRole;
import com.eventhub.identity.repository.RefreshTokenRepository;
import com.eventhub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthResponse signup(AuthRequest.Signup request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserRole role = UserRole.USER;
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
                if (role == UserRole.ADMIN) role = UserRole.USER; // prevent self-admin
            } catch (IllegalArgumentException ignored) {}
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .role(role)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        publishUserEvent("user.registered", user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest.Login request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        // Rotate refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String userId) {
        UUID uid = UUID.fromString(userId);
        refreshTokenRepository.revokeAllByUserId(uid);
        log.info("User logged out: {}", userId);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserDto getCurrentUser(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToUserDto(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .user(mapToUserDto(user))
                .tokens(AuthResponse.TokensDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenStr)
                        .build())
                .build();
    }

    private AuthResponse.UserDto mapToUserDto(User user) {
        return AuthResponse.UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .interests(user.getInterests())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    private void publishUserEvent(String eventType, User user) {
        try {
            kafkaTemplate.send("user-events", user.getId().toString(), Map.of(
                    "eventType", eventType,
                    "userId", user.getId().toString(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish user event: {}", e.getMessage());
        }
    }
}
