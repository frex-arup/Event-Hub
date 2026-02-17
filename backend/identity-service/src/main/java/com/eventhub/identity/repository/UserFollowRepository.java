package com.eventhub.identity.repository;

import com.eventhub.identity.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollow.UserFollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :userId")
    Page<UUID> findFollowingIds(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :userId")
    Page<UUID> findFollowerIds(@Param("userId") UUID userId, Pageable pageable);

    long countByFollowerId(UUID followerId);

    long countByFollowingId(UUID followingId);
}
