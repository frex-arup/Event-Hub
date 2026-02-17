package com.eventhub.social.repository;

import com.eventhub.social.entity.DirectMessage;
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
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    @Query("SELECT dm FROM DirectMessage dm WHERE " +
           "(dm.senderId = :userId1 AND dm.receiverId = :userId2) OR " +
           "(dm.senderId = :userId2 AND dm.receiverId = :userId1) " +
           "ORDER BY dm.createdAt ASC")
    Page<DirectMessage> findConversation(@Param("userId1") UUID userId1,
                                          @Param("userId2") UUID userId2,
                                          Pageable pageable);

    @Query("SELECT DISTINCT CASE WHEN dm.senderId = :userId THEN dm.receiverId ELSE dm.senderId END " +
           "FROM DirectMessage dm WHERE dm.senderId = :userId OR dm.receiverId = :userId")
    List<UUID> findConversationPartners(@Param("userId") UUID userId);

    long countByReceiverIdAndReadFalse(UUID receiverId);

    @Modifying
    @Query("UPDATE DirectMessage dm SET dm.read = true WHERE dm.receiverId = :receiverId AND dm.senderId = :senderId AND dm.read = false")
    int markConversationAsRead(@Param("receiverId") UUID receiverId, @Param("senderId") UUID senderId);
}
