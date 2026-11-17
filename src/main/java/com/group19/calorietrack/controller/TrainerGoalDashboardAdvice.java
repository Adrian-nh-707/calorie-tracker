
package com.group19.calorietrack.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.group19.calorietrack.services.TrainerGoalRecommendationService;
import com.group19.calorietrack.services.TrainerGoalRecommendationService.RecommendationView;

@ControllerAdvice(assignableTypes = DashboardController.class)
public class TrainerGoalDashboardAdvice {

    private final TrainerGoalRecommendationService recommendationService;

    public TrainerGoalDashboardAdvice(
            TrainerGoalRecommendationService recommendationService) {

        this.recommendationService = recommendationService;
    }

    //Load recommendations for the loggedin user
    @ModelAttribute("trainerGoalRecommendations")
    public List<RecommendationView> loadRecommendations(
            Principal principal) {

        if (principal == null) {
            return List.of();
        }

        //Only return recommendations belonging to this user
        return recommendationService.getRecommendationsForUser(
                principal.getName()
        );
    }
}
