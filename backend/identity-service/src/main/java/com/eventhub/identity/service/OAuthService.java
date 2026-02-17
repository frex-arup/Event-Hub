package com.eventhub.identity.service;

import com.eventhub.identity.entity.User;
import com.eventhub.identity.entity.UserRole;
import com.eventhub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Handles OAuth2 authentication for Google and GitHub providers.
 * Flow:
 * 1. Frontend redirects to provider's OAuth consent screen
 * 2. Provider redirects back with authorization code
 * 3. Frontend sends code to /api/v1/auth/oauth/{provider}
 * 4. This service exchanges code for tokens, fetches user info, creates/links account
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public Map<String, Object> authenticateWithOAuth(String provider, String code, String redirectUri) {
        return switch (provider.toLowerCase()) {
            case "google" -> handleGoogleOAuth(code, redirectUri);
            case "github" -> handleGithubOAuth(code, redirectUri);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };
    }

    private Map<String, Object> handleGoogleOAuth(String code, String redirectUri) {
        // Exchange code for access token
        // In production: use actual Google OAuth2 client credentials
        String googleClientId = System.getenv("GOOGLE_CLIENT_ID");
        String googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

        if (googleClientId == null || googleClientSecret == null) {
            throw new IllegalStateException("Google OAuth credentials not configured");
        }

        try {
            // Exchange authorization code for tokens
            Map<String, String> tokenRequest = Map.of(
                    "code", code,
                    "client_id", googleClientId,
                    "client_secret", googleClientSecret,
                    "redirect_uri", redirectUri,
                    "grant_type", "authorization_code"
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", tokenRequest, Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new IllegalStateException("Failed to exchange Google auth code");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // Fetch user info
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restTemplate.getForObject(
                    "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken,
                    Map.class);

            if (userInfo == null) {
                throw new IllegalStateException("Failed to fetch Google user info");
            }

            String oauthId = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String avatarUrl = (String) userInfo.get("picture");

            return findOrCreateOAuthUser("google", oauthId, email, name, avatarUrl);

        } catch (Exception e) {
            log.error("Google OAuth failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Google authentication failed: " + e.getMessage());
        }
    }

    private Map<String, Object> handleGithubOAuth(String code, String redirectUri) {
        String githubClientId = System.getenv("GITHUB_CLIENT_ID");
        String githubClientSecret = System.getenv("GITHUB_CLIENT_SECRET");

        if (githubClientId == null || githubClientSecret == null) {
            throw new IllegalStateException("GitHub OAuth credentials not configured");
        }

        try {
            // Exchange authorization code for access token
            Map<String, String> tokenRequest = Map.of(
                    "code", code,
                    "client_id", githubClientId,
                    "client_secret", githubClientSecret,
                    "redirect_uri", redirectUri
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    "https://github.com/login/oauth/access_token", tokenRequest, Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new IllegalStateException("Failed to exchange GitHub auth code");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // Fetch user info
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restTemplate.getForObject(
                    "https://api.github.com/user",
                    Map.class);

            if (userInfo == null) {
                throw new IllegalStateException("Failed to fetch GitHub user info");
            }

            String oauthId = String.valueOf(userInfo.get("id"));
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            if (name == null) name = (String) userInfo.get("login");
            String avatarUrl = (String) userInfo.get("avatar_url");

            // GitHub may not return email in main profile
            if (email == null) {
                email = oauthId + "@github.oauth";
            }

            return findOrCreateOAuthUser("github", oauthId, email, name, avatarUrl);

        } catch (Exception e) {
            log.error("GitHub OAuth failed: {}", e.getMessage(), e);
            throw new IllegalStateException("GitHub authentication failed: " + e.getMessage());
        }
    }

    private Map<String, Object> findOrCreateOAuthUser(String provider, String oauthId,
                                                       String email, String name, String avatarUrl) {
        // Check if user already linked this OAuth account
        User user = userRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .orElseGet(() -> {
                    // Check if user with same email exists (link accounts)
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                existingUser.setOauthProvider(provider);
                                existingUser.setOauthId(oauthId);
                                if (existingUser.getAvatarUrl() == null) {
                                    existingUser.setAvatarUrl(avatarUrl);
                                }
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                // Create new user
                                User newUser = new User();
                                newUser.setEmail(email);
                                newUser.setName(name);
                                newUser.setAvatarUrl(avatarUrl);
                                newUser.setOauthProvider(provider);
                                newUser.setOauthId(oauthId);
                                newUser.setRole(UserRole.USER);
                                newUser.setEmailVerified(true);
                                newUser.setEnabled(true);
                                User saved = userRepository.save(newUser);

                                // Publish user registered event
                                publishUserEvent("user.registered", saved);
                                return saved;
                            });
                });

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("OAuth {} login successful for user {} ({})", provider, user.getId(), user.getEmail());

        return Map.of(
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                ),
                "tokens", Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken
                )
        );
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
