
package com.group19.calorietrack.repositories;

import java.util.List;

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
}
