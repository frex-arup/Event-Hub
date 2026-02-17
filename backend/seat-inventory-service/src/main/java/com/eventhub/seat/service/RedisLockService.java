package com.eventhub.seat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis-based distributed lock service for seat reservation.
 * 
 * Lock strategy:
 * - Each seat lock is a Redis key: seat:lock:{eventId}:{seatId}
 * - Value is the userId who holds the lock
 * - TTL ensures locks auto-expire (survives service restarts)
 * - Atomic Lua scripts prevent race conditions
 * 
 * This prevents seat overselling under 10K+ concurrent users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_AVAILABILITY_PREFIX = "seat:avail:";
    private static final String USER_LOCKS_PREFIX = "user:locks:";

    // Lua script for atomic multi-seat locking
    // Returns: "OK" if all seats locked, or the ID of the first seat that failed
    private static final String LOCK_SEATS_SCRIPT = """
            local lockPrefix = ARGV[1]
            local userId = ARGV[2]
            local ttl = tonumber(ARGV[3])
            local userLocksKey = ARGV[4]
            local maxUserSeats = tonumber(ARGV[5])
            
            -- Check if user already has too many locks
            local currentUserLocks = redis.call('SCARD', userLocksKey)
            if currentUserLocks + #KEYS > maxUserSeats then
                return 'MAX_SEATS_EXCEEDED'
            end
            
            -- First pass: check all seats are available
            for i, seatKey in ipairs(KEYS) do
                local existing = redis.call('GET', seatKey)
                if existing ~= false and existing ~= userId then
                    return 'SEAT_UNAVAILABLE:' .. seatKey
                end
            end
            
            -- Second pass: lock all seats atomically
            for i, seatKey in ipairs(KEYS) do
                redis.call('SET', seatKey, userId, 'EX', ttl)
                redis.call('SADD', userLocksKey, seatKey)
            end
            redis.call('EXPIRE', userLocksKey, ttl)
            
            return 'OK'
            """;

    // Lua script for atomic multi-seat release
    private static final String RELEASE_SEATS_SCRIPT = """
            local userId = ARGV[1]
            local userLocksKey = ARGV[2]
            local released = 0
            
            for i, seatKey in ipairs(KEYS) do
                local holder = redis.call('GET', seatKey)
                if holder == userId then
                    redis.call('DEL', seatKey)
                    redis.call('SREM', userLocksKey, seatKey)
                    released = released + 1
                end
            end
            
            return released
            """;

    /**
     * Atomically lock multiple seats for a user using Lua script.
     * Prevents race conditions under high concurrency.
     */
    public LockResult lockSeats(UUID eventId, List<UUID> seatIds, UUID userId, int ttlSeconds, int maxSeatsPerUser) {
        List<String> keys = seatIds.stream()
                .map(seatId -> SEAT_LOCK_PREFIX + eventId + ":" + seatId)
                .collect(Collectors.toList());

        String userLocksKey = USER_LOCKS_PREFIX + eventId + ":" + userId;

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(LOCK_SEATS_SCRIPT);
        script.setResultType(String.class);

        try {
            String result = redisTemplate.execute(
                    script,
                    keys,
                    SEAT_LOCK_PREFIX + eventId + ":",
                    userId.toString(),
                    String.valueOf(ttlSeconds),
                    userLocksKey,
                    String.valueOf(maxSeatsPerUser)
            );

            if ("OK".equals(result)) {
                log.info("Seats locked successfully: eventId={}, userId={}, seats={}", eventId, userId, seatIds.size());
                return LockResult.success(UUID.randomUUID().toString());
            } else if (result != null && result.startsWith("MAX_SEATS_EXCEEDED")) {
                log.warn("User {} exceeded max seats for event {}", userId, eventId);
                return LockResult.failure("Maximum seat limit exceeded");
            } else {
                log.warn("Seat lock failed for event {}: {}", eventId, result);
                return LockResult.failure("One or more seats are no longer available");
            }
        } catch (Exception e) {
            log.error("Redis lock error for event {}: {}", eventId, e.getMessage(), e);
            return LockResult.failure("Lock service temporarily unavailable");
        }
    }

    /**
     * Atomically release seats locked by a user.
     */
    public int releaseSeats(UUID eventId, List<UUID> seatIds, UUID userId) {
        List<String> keys = seatIds.stream()
                .map(seatId -> SEAT_LOCK_PREFIX + eventId + ":" + seatId)
                .collect(Collectors.toList());

        String userLocksKey = USER_LOCKS_PREFIX + eventId + ":" + userId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RELEASE_SEATS_SCRIPT);
        script.setResultType(Long.class);

        try {
            Long released = redisTemplate.execute(script, keys, userId.toString(), userLocksKey);
            int count = released != null ? released.intValue() : 0;
            log.info("Released {} seats for user {} on event {}", count, userId, eventId);
            return count;
        } catch (Exception e) {
            log.error("Redis release error: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Check if a specific seat is locked.
     */
    public Optional<String> getSeatLockHolder(UUID eventId, UUID seatId) {
        String key = SEAT_LOCK_PREFIX + eventId + ":" + seatId;
        String holder = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(holder);
    }

    /**
     * Get all seat IDs locked by a user for an event.
     */
    public Set<String> getUserLockedSeats(UUID eventId, UUID userId) {
        String userLocksKey = USER_LOCKS_PREFIX + eventId + ":" + userId;
        Set<String> members = redisTemplate.opsForSet().members(userLocksKey);
        return members != null ? members : Set.of();
    }

    /**
     * Cache seat availability counts in Redis for fast reads.
     */
    public void cacheAvailability(UUID eventId, String sectionId, long available, long total) {
        String key = SEAT_AVAILABILITY_PREFIX + eventId + ":" + sectionId;
        Map<String, String> data = Map.of(
                "available", String.valueOf(available),
                "total", String.valueOf(total)
        );
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, Duration.ofSeconds(30));
    }

    /**
     * Get cached availability for a section.
     */
    public Optional<Map<String, String>> getCachedAvailability(UUID eventId, String sectionId) {
        String key = SEAT_AVAILABILITY_PREFIX + eventId + ":" + sectionId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) return Optional.empty();

        Map<String, String> result = new HashMap<>();
        data.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return Optional.of(result);
    }

    // ─────────────────────────────────────────────
    // Result type
    // ─────────────────────────────────────────────

    public record LockResult(boolean success, String lockId, String errorMessage) {
        public static LockResult success(String lockId) {
            return new LockResult(true, lockId, null);
        }
        public static LockResult failure(String message) {
            return new LockResult(false, null, message);
        }
    }
}
