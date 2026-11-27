
package com.group19.calorietrack.repositories;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.group19.calorietrack.models.TrainerGoalRecommendation;

public interface TrainerGoalRecommendationRepository
        extends JpaRepository<TrainerGoalRecommendation, Long> {

    // Get one users' recommendations newest first
    @EntityGraph(attributePaths = {"trainer"})
    List<TrainerGoalRecommendation> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    @EntityGraph(attributePaths = {"trainer", "user"})
    List<TrainerGoalRecommendation> findByTrainer_IdOrderByCreatedAtDesc(Long trainerId);

    // Serialize responses to the same recommendation, including concurrent requests.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TrainerGoalRecommendation r where r.id = :id")
    Optional<TrainerGoalRecommendation> findForResponse(@Param("id") Long id);
}
