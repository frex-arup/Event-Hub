package com.eventhub.identity.service;

import com.eventhub.identity.dto.AuthRequest;
import com.eventhub.identity.dto.AuthResponse;
import com.eventhub.identity.entity.RefreshToken;
import com.eventhub.identity.entity.User;
import com.eventhub.identity.entity.UserRole;
import com.eventhub.identity.repository.RefreshTokenRepository;
import com.eventhub.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private AuthService authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(jwtService.generateAccessToken(any(User.class)))
                .thenReturn("mock-access-token");
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private User buildUser(String email, String name) {
        return User.builder()
                .id(userId)
                .email(email)
                .name(name)
                .passwordHash("hashed-pw")
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // Signup
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("signup")
    class SignupTests {

        @Test
        @DisplayName("should create user and return auth response")
        void shouldSignupSuccessfully() {
            AuthRequest.Signup req = new AuthRequest.Signup();
            req.setName("Alice");
            req.setEmail("alice@test.com");
            req.setPassword("password123");

            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);
                u.setCreatedAt(Instant.now());
                return u;
            });

            AuthResponse response = authService.signup(req);

            assertThat(response.getUser().getEmail()).isEqualTo("alice@test.com");
            assertThat(response.getUser().getRole()).isEqualTo("USER");
            assertThat(response.getTokens().getAccessToken()).isEqualTo("mock-access-token");
            assertThat(response.getTokens().getRefreshToken()).isNotBlank();
            verify(kafkaTemplate).send(eq("user-events"), anyString(), any());
        }

        @Test
        @DisplayName("should throw if email already registered")
        void shouldThrowIfDuplicateEmail() {
            AuthRequest.Signup req = new AuthRequest.Signup();
            req.setName("Bob");
            req.setEmail("bob@test.com");
            req.setPassword("password123");

            when(userRepository.existsByEmail("bob@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should prevent self-admin role assignment")
        void shouldPreventAdminRole() {
            AuthRequest.Signup req = new AuthRequest.Signup();
            req.setName("Mallory");
            req.setEmail("mallory@test.com");
            req.setPassword("password123");
            req.setRole("ADMIN");

            when(userRepository.existsByEmail("mallory@test.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);
                u.setCreatedAt(Instant.now());
                return u;
            });

            AuthResponse response = authService.signup(req);

            assertThat(response.getUser().getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should allow ORGANIZER role assignment")
        void shouldAllowOrganizerRole() {
            AuthRequest.Signup req = new AuthRequest.Signup();
            req.setName("Org");
            req.setEmail("org@test.com");
            req.setPassword("password123");
            req.setRole("ORGANIZER");

            when(userRepository.existsByEmail("org@test.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);
                u.setCreatedAt(Instant.now());
                return u;
            });

            AuthResponse response = authService.signup(req);

            assertThat(response.getUser().getRole()).isEqualTo("ORGANIZER");
        }
    }

    // ─────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should login with correct credentials")
        void shouldLoginSuccessfully() {
            AuthRequest.Login req = new AuthRequest.Login();
            req.setEmail("alice@test.com");
            req.setPassword("password123");

            User user = buildUser("alice@test.com", "Alice");
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);

            AuthResponse response = authService.login(req);

            assertThat(response.getUser().getEmail()).isEqualTo("alice@test.com");
            assertThat(response.getTokens().getAccessToken()).isNotBlank();
        }

        @Test
        @DisplayName("should throw on wrong password")
        void shouldThrowOnWrongPassword() {
            AuthRequest.Login req = new AuthRequest.Login();
            req.setEmail("alice@test.com");
            req.setPassword("wrong");

            User user = buildUser("alice@test.com", "Alice");
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed-pw")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should throw on non-existent email")
        void shouldThrowOnUnknownEmail() {
            AuthRequest.Login req = new AuthRequest.Login();
            req.setEmail("unknown@test.com");
            req.setPassword("password");

            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should throw on disabled account")
        void shouldThrowOnDisabledAccount() {
            AuthRequest.Login req = new AuthRequest.Login();
            req.setEmail("disabled@test.com");
            req.setPassword("password");

            User user = buildUser("disabled@test.com", "Disabled");
            user.setEnabled(false);
            when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("disabled");
        }
    }

    // ─────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should revoke all refresh tokens for user")
        void shouldLogout() {
            authService.logout(userId.toString());

            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }
}
