package com.group19.calorietrack.services;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group19.calorietrack.models.TrainerGoalRecommendation;
import com.group19.calorietrack.models.RecommendationStatus;
import com.group19.calorietrack.messaging.MessageService;
import com.group19.calorietrack.repositories.TrainerGoalRecommendationRepository;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Service
public class TrainerGoalRecommendationService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final TrainerGoalRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public TrainerGoalRecommendationService(
            TrainerGoalRecommendationRepository recommendationRepository,
            UserRepository userRepository,
            MessageService messageService) {

        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    @Transactional
    public TrainerGoalRecommendation createRecommendation(
            String trainerEmail,
            Long userId,
            Integer calorieGoal,
            Integer proteinGoal,
            String note) {

        // Check trainer authentication
        if (trainerEmail == null || trainerEmail.isBlank()) {
            throw new AccessDeniedException(
                    "Trainer authentication is required."
            );
        }

        AppUser trainer = userRepository.findByEmail(trainerEmail)
                .orElseThrow(() -> new AccessDeniedException(
                        "Trainer account not found."
                ));

        // Only trainers can send recommendations
        if (trainer.getRole() != UserRole.TRAINER) {
            throw new AccessDeniedException(
                    "Only trainers can create recommendations."
            );
        }

        // Validate the selected user
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Please select a user."
            );
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User account not found."
                ));

        if (user.getRole() != UserRole.USER) {
            throw new IllegalArgumentException(
                    "Recommendations can only be sent to regular users."
            );
        }

        // Check the trainer is assigned to this user
        AppUser assignedTrainer = user.getAssignedTrainer();

        if (assignedTrainer == null ||
                !Objects.equals(assignedTrainer.getId(), trainer.getId())) {

            throw new AccessDeniedException(
                    "You can only recommend goals to your assigned users."
            );
        }

        // Validate calorie target
        if (calorieGoal == null ||
                calorieGoal < 1 ||
                calorieGoal > 10000) {

            throw new IllegalArgumentException(
                    "Calorie goal must be between 1 and 10000."
            );
        }

        // Validateprotein target
        if (proteinGoal == null ||
                proteinGoal < 0 ||
                proteinGoal > 500) {

            throw new IllegalArgumentException(
                    "Protein goal must be between 0 and 500."
            );
        }

        //Clean up the trainer's note
        String cleanedNote = note == null ? "" : note.trim();

        if (cleanedNote.length() > 1000) {
            throw new IllegalArgumentException(
                    "The recommendation note cannot exceed 1000 characters."
            );
        }


        TrainerGoalRecommendation recommendation = // Save the recommendation
                new TrainerGoalRecommendation(
                        trainer,
                        user,
                        calorieGoal,
                        proteinGoal,
                        cleanedNote
                );

        TrainerGoalRecommendation saved = recommendationRepository.save(recommendation);
        messageService.sendMessage(trainerEmail, user.getId(),
                "New nutrition goal recommendation",
                "Your trainer recommended " + calorieGoal + " kcal/day and " + proteinGoal
                        + " g protein/day. Open your dashboard to review the recommendation.");
        return saved;
    }

    //Get recommendations for the logged-in regular user
    @Transactional(readOnly = true)
    public List<RecommendationView> getRecommendationsForUser(
            String userEmail) {

        if (userEmail == null || userEmail.isBlank()) {
            throw new AccessDeniedException(
                    "User authentication is required."
            );
        }

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException(
                        "User account not found."
                ));

        if (user.getRole() != UserRole.USER) {
            throw new AccessDeniedException(
                    "Only regular users can view these recommendations."
            );
        }

        // Get this user's recommendations, newest first
        List<TrainerGoalRecommendation> recommendations =
                recommendationRepository.findByUser_IdOrderByCreatedAtDesc(
                        user.getId()
                );

        return recommendations.stream() // Prepare the values used by the dashboard
                .map(this::toRecommendationView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationView> getRecommendationsForTrainer(String trainerEmail) {
        AppUser trainer = userRepository.findByEmail(trainerEmail)
                .orElseThrow(() -> new AccessDeniedException("Trainer account not found."));
        if (trainer.getRole() != UserRole.TRAINER) {
            throw new AccessDeniedException("Only trainers can view sent recommendations.");
        }
        return recommendationRepository.findByTrainer_IdOrderByCreatedAtDesc(trainer.getId())
                .stream().map(this::toRecommendationView).toList();
    }

    @Transactional
    public void respondToRecommendation(String userEmail, Long recommendationId,
                                        RecommendationStatus decision, String reason) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new AccessDeniedException("User authentication is required.");
        }
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));
        if (user.getRole() != UserRole.USER) {
            throw new AccessDeniedException("Only regular users can respond.");
        }
        if (decision != RecommendationStatus.ACCEPTED && decision != RecommendationStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid recommendation decision.");
        }
        String cleanedReason = decision == RecommendationStatus.REJECTED && reason != null
                ? reason.trim() : null;
        if (cleanedReason != null && cleanedReason.length() > 1000) {
            throw new IllegalArgumentException("Feedback cannot exceed 1000 characters.");
        }
        TrainerGoalRecommendation recommendation = recommendationRepository.findForResponse(recommendationId)
                .orElseThrow(() -> new AccessDeniedException("Recommendation not found or access denied."));
        if (!Objects.equals(recommendation.getUser().getId(), user.getId())) {
            throw new AccessDeniedException("Recommendation not found or access denied.");
        }
        // The row lock and state check prevent duplicate decisions and messages.
        recommendation.respond(decision, cleanedReason);
        if (decision == RecommendationStatus.ACCEPTED) {
            user.setDailyCalorieGoal(recommendation.getCalorieGoal());
            user.setDailyProteinGoal(recommendation.getProteinGoal());
            userRepository.save(user);
        }
        recommendationRepository.save(recommendation);
        // The messaging service allows only currently assigned trainer/user pairs.
        // Old recommendations remain answerable after reassignment, without a new message.
        AppUser assigned = user.getAssignedTrainer();
        if (assigned != null && Objects.equals(assigned.getId(), recommendation.getTrainer().getId())) {
            String subject = decision == RecommendationStatus.ACCEPTED
                    ? "Nutrition goals accepted" : "Nutrition goals rejected";
            String body = decision == RecommendationStatus.ACCEPTED
                    ? "Your client accepted your recommendation. Their nutrition targets have been updated."
                    : "Your client rejected your recommendation. Their nutrition targets remain unchanged.";
            body += " Recommended: " + recommendation.getCalorieGoal() + " kcal/day, "
                    + recommendation.getProteinGoal() + " g protein/day.";
            if (cleanedReason != null && !cleanedReason.isBlank()) {
                body += " Feedback: " + cleanedReason;
            }
            messageService.sendMessage(userEmail, assigned.getId(), subject, body);
        }
    }

    private RecommendationView toRecommendationView( //Convert a database record into dashboard information
            TrainerGoalRecommendation recommendation) {

        AppUser trainer = recommendation.getTrainer();

        String trainerName = (
                (trainer.getFirstName() == null
                        ? "" : trainer.getFirstName()) + " " +
                (trainer.getLastName() == null
                        ? "" : trainer.getLastName())
        ).trim();

        if (trainerName.isEmpty()) {
            trainerName = "Your Trainer";
        }

        String dateLabel = recommendation.getCreatedAt() == null
                ? ""
                : recommendation.getCreatedAt().format(DATE_FORMAT);

        return new RecommendationView(
                trainerName,
                recommendation.getCalorieGoal(),
                recommendation.getProteinGoal(),
                recommendation.getNote(),
                dateLabel,
                recommendation.getId(),
                recommendation.getStatus().name(),
                recommendation.getRespondedAt() == null ? "" : recommendation.getRespondedAt().format(DATE_FORMAT),
                recommendation.getRejectionReason(),
                ((recommendation.getUser().getFirstName() == null ? "" : recommendation.getUser().getFirstName())
                        + " " + (recommendation.getUser().getLastName() == null ? "" : recommendation.getUser().getLastName())).trim()
        );
    }

    //Fields expected by the user dashboard HTML
    public record RecommendationView(
            String trainerName,
            int calorieGoal,
            int proteinGoal,
            String note,
            String createdAtLabel,
            Long id,
            String status,
            String respondedAtLabel,
            String rejectionReason,
            String userName
    ) {
    }
}
