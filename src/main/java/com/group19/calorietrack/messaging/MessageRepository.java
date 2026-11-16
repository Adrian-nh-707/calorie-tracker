package com.group19.calorietrack.messaging;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    Optional<Message> findByIdAndRecipient_Id(Long messageId, Long recipientId);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);
}