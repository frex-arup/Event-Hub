package com.eventhub.notification.repository;

import com.eventhub.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false WHERE d.token = :token")
    void deactivateToken(String token);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false WHERE d.userId = :userId")
    void deactivateAllForUser(UUID userId);

    void deleteByUserIdAndToken(UUID userId, String token);
}
