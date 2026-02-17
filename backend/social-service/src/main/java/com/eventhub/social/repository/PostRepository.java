package com.eventhub.social.repository;

import com.eventhub.social.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<Post> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.authorId IN :authorIds ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorIdIn(@Param("authorIds") List<UUID> authorIds, Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<Post> findTrending(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikes(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = GREATEST(p.likesCount - 1, 0) WHERE p.id = :postId")
    void decrementLikes(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId")
    void incrementComments(@Param("postId") UUID postId);
}
