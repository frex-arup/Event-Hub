package com.eventhub.identity.service;

import com.eventhub.identity.entity.User;
import com.eventhub.identity.entity.UserFollow;
import com.eventhub.identity.repository.UserFollowRepository;
import com.eventhub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public User updateProfile(UUID userId, String name, String bio, String avatarUrl) {
        User user = getProfile(userId);
        if (name != null && !name.isBlank()) user.setName(name);
        if (bio != null) user.setBio(bio);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        user = userRepository.save(user);
        log.info("Profile updated for user {}", userId);
        return user;
    }

    @Transactional
    public User updateInterests(UUID userId, Set<String> interests) {
        User user = getProfile(userId);
        user.setInterests(interests);
        user = userRepository.save(user);
        log.info("Interests updated for user {}: {}", userId, interests);

        // Publish signal for recommendation engine
        try {
            kafkaTemplate.send("recommendation-signals", userId.toString(), Map.of(
                    "signalType", "user.interests.updated",
                    "userId", userId.toString(),
                    "interests", interests.stream().toList(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish interests update signal: {}", e.getMessage());
        }

        return user;
    }

    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        if (!userRepository.existsById(followingId)) {
            throw new IllegalArgumentException("User not found: " + followingId);
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return; // Already following — idempotent
        }
        followRepository.save(UserFollow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build());
        log.info("User {} now follows {}", followerId, followingId);

        // Notify the followed user
        try {
            kafkaTemplate.send("notification-events", followingId.toString(), Map.of(
                    "eventType", "new.follower",
                    "userId", followingId.toString(),
                    "followerId", followerId.toString(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish follow notification: {}", e.getMessage());
        }
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("User {} unfollowed {}", followerId, followingId);
    }

    @Transactional(readOnly = true)
    public Page<UUID> getFollowing(UUID userId, int page, int size) {
        return followRepository.findFollowingIds(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<UUID> getFollowers(UUID userId, int page, int size) {
        return followRepository.findFollowerIds(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getFollowCounts(UUID userId) {
        long followers = followRepository.countByFollowingId(userId);
        long following = followRepository.countByFollowerId(userId);
        return Map.of("followers", followers, "following", following);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
}
