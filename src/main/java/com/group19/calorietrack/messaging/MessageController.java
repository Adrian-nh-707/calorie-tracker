package com.group19.calorietrack.messaging;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Controller
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageController(
            MessageService messageService,
            UserRepository userRepository) {

        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @GetMapping("/inbox")
    public String inbox(
            Authentication authentication,
            Model model) {

        try {
            model.addAttribute(
                    "messages",
                    messageService.getInbox(authentication.getName()));

            model.addAttribute(
                    "unreadCount",
                    messageService.getUnreadCount(authentication.getName()));

            return "inbox";

        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/inbox/{messageId}")
    public String openMessage(
            @PathVariable Long messageId,
            Authentication authentication,
            Model model) {

        try {
            Message message = messageService.openMessage(
                    messageId, authentication.getName());

            model.addAttribute("message", message);

            return "message-details";

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Message not found...");

        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/messages/new")
    public String newMessage(
            Authentication authentication,
            Model model) {

        AppUser sender = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED));

        List<AppUser> recipients;

        if (sender.getRole() == UserRole.TRAINER) {

            recipients = userRepository.findByAssignedTrainer_IdAndRole(
                    sender.getId(), UserRole.USER);

        } else if (sender.getRole() == UserRole.USER) {

            if (sender.getAssignedTrainer() != null) {
                recipients = List.of(sender.getAssignedTrainer());
            } else {
                recipients = List.of();
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account cannot use messaging...");
        }

        model.addAttribute("recipients", recipients);

        return "send-message";
    }

    @PostMapping("/messages")
    public String sendMessage(
            @RequestParam Long recipientId,
            @RequestParam String subject,
            @RequestParam String content,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            messageService.sendMessage(
                    authentication.getName(),
                    recipientId,
                    subject,
                    content);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Message sent successfully.");

            return "redirect:/inbox";

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

            return "redirect:/messages/new";
        }
    }
}