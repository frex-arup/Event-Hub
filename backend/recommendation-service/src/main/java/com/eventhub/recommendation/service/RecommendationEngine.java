package com.eventhub.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation engine that builds user interest profiles
 * from Kafka signals and provides event suggestions.
 *
 * Strategies:
 * 1. Interest-based: matches user interests → event tags/categories
 * 2. Social-graph: recommends events popular among user's connections
 * 3. Trending: globally popular events (booking velocity)
 * 4. Collaborative filtering: users who booked X also booked Y
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEngine {

    private final StringRedisTemplate redisTemplate;

    private static final String USER_INTERESTS_KEY = "rec:interests:";
    private static final String EVENT_POPULARITY_KEY = "rec:popularity";
    private static final String USER_BOOKINGS_KEY = "rec:bookings:";
    private static final String CATEGORY_EVENTS_KEY = "rec:category:";

    // ─────────────────────────────────────────────
    // Kafka Signal Consumers
    // ─────────────────────────────────────────────

    @KafkaListener(topics = "recommendation-signals", groupId = "recommendation-service-group")
    public void handleSignal(Map<String, Object> signal) {
        String signalType = (String) signal.get("signalType");
        if (signalType == null) return;

        try {
            switch (signalType) {
                case "event.viewed" -> handleEventViewed(signal);
                case "event.booked" -> handleEventBooked(signal);
                case "event.searched" -> handleEventSearched(signal);
                case "user.interests.updated" -> handleInterestsUpdated(signal);
                default -> log.trace("Unknown signal type: {}", signalType);
            }
        } catch (Exception e) {
            log.warn("Error processing recommendation signal: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking-events", groupId = "recommendation-service-group")
    public void handleBookingEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if ("booking.requested".equals(eventType) || "booking.confirmed".equals(eventType)) {
            String eventId = (String) event.get("eventId");
            String userId = (String) event.get("userId");

            // Increase event popularity score
            redisTemplate.opsForZSet().incrementScore(EVENT_POPULARITY_KEY, eventId, 1.0);

            // Track user booking history
            if (userId != null) {
                redisTemplate.opsForSet().add(USER_BOOKINGS_KEY + userId, eventId);
            }
        }
    }

    // ─────────────────────────────────────────────
    // Recommendation Queries
    // ─────────────────────────────────────────────

    /**
     * Get personalized event recommendations for a user.
     */
    public List<String> getRecommendations(String userId, int limit) {
        Set<String> recommendations = new LinkedHashSet<>();

        // 1. Interest-based recommendations
        Set<String> interests = redisTemplate.opsForSet().members(USER_INTERESTS_KEY + userId);
        if (interests != null) {
            for (String interest : interests) {
                Set<String> categoryEvents = redisTemplate.opsForSet()
                        .members(CATEGORY_EVENTS_KEY + interest.toLowerCase());
                if (categoryEvents != null) {
                    recommendations.addAll(categoryEvents);
                }
                if (recommendations.size() >= limit * 2) break;
            }
        }

        // 2. Trending events (fill remaining slots)
        Set<ZSetOperations.TypedTuple<String>> trending = redisTemplate.opsForZSet()
                .reverseRangeWithScores(EVENT_POPULARITY_KEY, 0, limit * 2);
        if (trending != null) {
            for (var entry : trending) {
                if (entry.getValue() != null) {
                    recommendations.add(entry.getValue());
                }
            }
        }

        // 3. Remove events user already booked
        Set<String> userBookings = redisTemplate.opsForSet().members(USER_BOOKINGS_KEY + userId);
        if (userBookings != null) {
            recommendations.removeAll(userBookings);
        }

        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get trending events globally.
     */
    public List<String> getTrendingEventIds(int limit) {
        Set<String> trending = redisTemplate.opsForZSet()
                .reverseRange(EVENT_POPULARITY_KEY, 0, limit - 1);
        return trending != null ? new ArrayList<>(trending) : List.of();
    }

    // ─────────────────────────────────────────────
    // Signal Handlers
    // ─────────────────────────────────────────────

    private void handleEventViewed(Map<String, Object> signal) {
        String eventId = (String) signal.get("eventId");
        if (eventId != null) {
            redisTemplate.opsForZSet().incrementScore(EVENT_POPULARITY_KEY, eventId, 0.1);
        }
    }

    private void handleEventBooked(Map<String, Object> signal) {
        String eventId = (String) signal.get("eventId");
        String userId = (String) signal.get("userId");
        if (eventId != null) {
            redisTemplate.opsForZSet().incrementScore(EVENT_POPULARITY_KEY, eventId, 1.0);
        }
        if (userId != null && eventId != null) {
            redisTemplate.opsForSet().add(USER_BOOKINGS_KEY + userId, eventId);
        }
    }

    private void handleEventSearched(Map<String, Object> signal) {
        String userId = (String) signal.get("userId");
        String query = (String) signal.get("query");
        if (userId != null && query != null) {
            redisTemplate.opsForSet().add(USER_INTERESTS_KEY + userId, query.toLowerCase());
            redisTemplate.expire(USER_INTERESTS_KEY + userId, Duration.ofDays(30));
        }
    }

    private void handleInterestsUpdated(Map<String, Object> signal) {
        String userId = (String) signal.get("userId");
        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) signal.get("interests");
        if (userId != null && interests != null) {
            String key = USER_INTERESTS_KEY + userId;
            redisTemplate.delete(key);
            for (String interest : interests) {
                redisTemplate.opsForSet().add(key, interest.toLowerCase());
            }
            redisTemplate.expire(key, Duration.ofDays(90));
        }
    }
}
