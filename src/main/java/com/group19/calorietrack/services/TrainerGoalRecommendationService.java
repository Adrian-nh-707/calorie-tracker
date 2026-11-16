package com.group19.calorietrack.services;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group19.calorietrack.models.TrainerGoalRecommendation;
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

    public TrainerGoalRecommendationService(
            TrainerGoalRecommendationRepository recommendationRepository,
            UserRepository userRepository) {

        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
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

        return recommendationRepository.save(recommendation);
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
                .limit(1)
                .map(this::toRecommendationView)
                .toList();
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
                dateLabel
        );
    }

    //Fields expected by the user dashboard HTML
    public record RecommendationView(
            String trainerName,
            int calorieGoal,
            int proteinGoal,
            String note,
            String createdAtLabel
    ) {
    }
}
