package com.eventhub.social.controller;

import com.eventhub.social.entity.Comment;
import com.eventhub.social.entity.Post;
import com.eventhub.social.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<Post>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getFeed(page, size));
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<Post>> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getTrending(page, size));
    }

    @GetMapping("/user/{authorId}")
    public ResponseEntity<Page<Post>> getUserPosts(
            @PathVariable UUID authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getUserPosts(authorId, page, size));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        UUID eventId = body.containsKey("eventId") ? UUID.fromString(body.get("eventId")) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(
                        UUID.fromString(userId),
                        body.get("content"),
                        body.get("imageUrl"),
                        eventId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") String userId) {
        postService.deletePost(postId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") String userId) {
        boolean liked = postService.toggleLike(postId, UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<Comment>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getComments(postId, page, size));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.addComment(postId, UUID.fromString(userId), body.get("content")));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") String userId) {
        postService.deleteComment(commentId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }
}
