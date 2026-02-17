package com.eventhub.recommendation.controller;

import com.eventhub.recommendation.service.RecommendationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationEngine recommendationEngine;

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<String> eventIds = recommendationEngine.getRecommendations(userId, limit);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "eventIds", eventIds,
                "count", eventIds.size()
        ));
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @RequestParam(defaultValue = "20") int limit) {
        List<String> eventIds = recommendationEngine.getTrendingEventIds(limit);
        return ResponseEntity.ok(Map.of(
                "eventIds", eventIds,
                "count", eventIds.size()
        ));
    }
}
