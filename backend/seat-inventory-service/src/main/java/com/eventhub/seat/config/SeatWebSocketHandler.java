package com.eventhub.seat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for broadcasting real-time seat status changes
 * to all connected clients watching a specific event.
 */
@Component
@Slf4j
public class SeatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // eventId -> set of connected sessions
    private final ConcurrentHashMap<String, Set<WebSocketSession>> eventSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String eventId = extractEventId(session);
        if (eventId != null) {
            eventSessions.computeIfAbsent(eventId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.debug("WS connected: session={} event={} total={}", session.getId(), eventId,
                    eventSessions.get(eventId).size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String eventId = extractEventId(session);
        if (eventId != null) {
            Set<WebSocketSession> sessions = eventSessions.get(eventId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    eventSessions.remove(eventId);
                }
            }
            log.debug("WS disconnected: session={} event={}", session.getId(), eventId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle PING from client
        try {
            Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if ("PING".equals(payload.get("type"))) {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
        } catch (Exception e) {
            log.trace("Ignoring invalid WS message from {}", session.getId());
        }
    }

    /**
     * Broadcast a seat event to all clients watching a specific event.
     */
    public void broadcastSeatEvent(String eventId, Map<String, Object> event) {
        Set<WebSocketSession> sessions = eventSessions.get(eventId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String payload = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(payload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Failed to send WS message to {}: {}", session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize WS event: {}", e.getMessage());
        }
    }

    public int getConnectionCount(String eventId) {
        Set<WebSocketSession> sessions = eventSessions.get(eventId);
        return sessions != null ? sessions.size() : 0;
    }

    private String extractEventId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        // Expected: /ws/seats/{eventId}
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }
}
