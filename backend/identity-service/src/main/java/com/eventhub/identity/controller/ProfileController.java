package com.eventhub.identity.controller;

import com.eventhub.identity.entity.User;
import com.eventhub.identity.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<User> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(profileService.updateProfile(
                UUID.fromString(userId),
                updates.get("name"),
                updates.get("bio"),
                updates.get("avatarUrl")));
    }

    @PutMapping("/interests")
    public ResponseEntity<User> updateInterests(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Set<String>> body) {
        Set<String> interests = body.getOrDefault("interests", Set.of());
        return ResponseEntity.ok(profileService.updateInterests(UUID.fromString(userId), interests));
    }

    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<Void> follow(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID targetUserId) {
        profileService.follow(UUID.fromString(userId), targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/follow/{targetUserId}")
    public ResponseEntity<Void> unfollow(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID targetUserId) {
        profileService.unfollow(UUID.fromString(userId), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UUID>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.getFollowing(userId, page, size));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UUID>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.getFollowers(userId, page, size));
    }

    @GetMapping("/{userId}/follow-counts")
    public ResponseEntity<Map<String, Long>> getFollowCounts(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getFollowCounts(userId));
    }

    @GetMapping("/{userId}/is-following/{targetUserId}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId) {
        return ResponseEntity.ok(Map.of("following", profileService.isFollowing(userId, targetUserId)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(404).body(Map.of(
                "status", 404, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }
}
