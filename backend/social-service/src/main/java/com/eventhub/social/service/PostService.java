package com.eventhub.social.service;

import com.eventhub.social.entity.Comment;
import com.eventhub.social.entity.Post;
import com.eventhub.social.entity.PostLike;
import com.eventhub.social.repository.CommentRepository;
import com.eventhub.social.repository.PostLikeRepository;
import com.eventhub.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public Page<Post> getFeed(int page, int size) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Post> getFollowingFeed(List<UUID> followingIds, int page, int size) {
        if (followingIds == null || followingIds.isEmpty()) {
            return Page.empty();
        }
        return postRepository.findByAuthorIdIn(followingIds, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Post> getTrending(int page, int size) {
        return postRepository.findTrending(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Post> getUserPosts(UUID authorId, int page, int size) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    }

    @Transactional
    public Post createPost(UUID authorId, String content, String imageUrl, UUID eventId) {
        Post post = Post.builder()
                .authorId(authorId)
                .content(content)
                .imageUrl(imageUrl)
                .eventId(eventId)
                .build();
        post = postRepository.save(post);
        log.info("Post created: {} by user {}", post.getId(), authorId);
        return post;
    }

    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = getPost(postId);
        if (!post.getAuthorId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this post");
        }
        postRepository.delete(post);
        log.info("Post deleted: {} by user {}", postId, userId);
    }

    @Transactional
    public boolean toggleLike(UUID postId, UUID userId) {
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            likeRepository.deleteByPostIdAndUserId(postId, userId);
            postRepository.decrementLikes(postId);
            return false; // unliked
        } else {
            likeRepository.save(PostLike.builder().postId(postId).userId(userId).build());
            postRepository.incrementLikes(postId);
            return true; // liked
        }
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(UUID postId, UUID userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }

    // ─── Comments ───

    @Transactional(readOnly = true)
    public Page<Comment> getComments(UUID postId, int page, int size) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(page, size));
    }

    @Transactional
    public Comment addComment(UUID postId, UUID authorId, String content) {
        // Verify post exists
        getPost(postId);

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content(content)
                .build();
        comment = commentRepository.save(comment);
        postRepository.incrementComments(postId);
        log.info("Comment added: {} on post {} by user {}", comment.getId(), postId, authorId);
        return comment;
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        log.info("Comment deleted: {} by user {}", commentId, userId);
    }
}
