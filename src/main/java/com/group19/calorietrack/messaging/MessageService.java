package com.group19.calorietrack.messaging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository) {

        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Message sendMessage(
            String senderEmail,
            Long recipientId,
            String subject,
            String content) {

        AppUser sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Sender account not found..."));

        AppUser recipient = userRepository.findById(recipientId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Recipient account not found..."));

        boolean senderAllowed =
                sender.getRole() == UserRole.USER ||
                sender.getRole() == UserRole.TRAINER;

        if (!senderAllowed) {
            throw new IllegalStateException(
                    "Your account is not permitted to send messages...");
        }

        boolean validAssignment = false;

        if (sender.getRole() == UserRole.TRAINER
                && recipient.getRole() == UserRole.USER) {

            AppUser assignedTrainer = recipient.getAssignedTrainer();

            validAssignment = assignedTrainer != null &&
                    Objects.equals(assignedTrainer.getId(), sender.getId());

        } else if (sender.getRole() == UserRole.USER
                && recipient.getRole() == UserRole.TRAINER) {

            AppUser assignedTrainer = sender.getAssignedTrainer();

            validAssignment = assignedTrainer != null &&
                    Objects.equals(assignedTrainer.getId(), recipient.getId());
        }

        if (!validAssignment) {
            throw new IllegalStateException(
                    "Messages can only be exchanged between assigned users and trainers...");
        }

        if (subject == null || subject.isBlank()
                || subject.length() > 150) {
            throw new IllegalArgumentException(
                    "Subject must contain between 1 and 150 characters...");
        }

        if (content == null || content.isBlank()
                || content.length() > 5000) {
            throw new IllegalArgumentException(
                    "Message must contain between 1 and 5000 characters...");
        }

        Message message = new Message();

        message.setSender(sender);
        message.setRecipient(recipient);
        message.setSubject(subject.trim());
        message.setContent(content.trim());

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getInbox(String email) {

        AppUser account = getMessagingAccount(email);

        return messageRepository
                .findByRecipient_IdOrderByCreatedAtDesc(account.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {

        AppUser account = getMessagingAccount(email);

        return messageRepository
                .countByRecipient_IdAndReadAtIsNull(account.getId());
    }

    @Transactional
    public Message openMessage(Long messageId, String email) {

        AppUser account = getMessagingAccount(email);

        Message message = messageRepository
                .findByIdAndRecipient_Id(messageId, account.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Message not found or access denied..."));

        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
        }

        return message;
    }

    private AppUser getMessagingAccount(String email) {

        AppUser account = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found..."));

        if (account.getRole() != UserRole.USER &&
                account.getRole() != UserRole.TRAINER) {

            throw new IllegalStateException(
                    "This account cannot access messaging...");
        }

        return account;
    }
}