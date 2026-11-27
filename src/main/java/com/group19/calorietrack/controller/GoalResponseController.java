package com.group19.calorietrack.controller;

import java.security.Principal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group19.calorietrack.models.RecommendationStatus;
import com.group19.calorietrack.services.TrainerGoalRecommendationService;

@Controller
public class GoalResponseController {
    private final TrainerGoalRecommendationService service;

    public GoalResponseController(TrainerGoalRecommendationService service) {
        this.service = service;
    }

    @PostMapping("/dashboard/recommendations/{id}/respond")
    public String respond(Principal principal, @PathVariable("id") Long id,
                          @RequestParam("decision") RecommendationStatus decision,
                          @RequestParam(value = "reason", required = false) String reason,
                          RedirectAttributes redirect) {
        if (principal == null) {
            throw new AccessDeniedException("User authentication is required.");
        }
        try {
            service.respondToRecommendation(principal.getName(), id, decision, reason);
            redirect.addFlashAttribute("goalSuccess", decision == RecommendationStatus.ACCEPTED
                    ? "Recommendation accepted. Your nutrition goals have been updated."
                    : "Recommendation rejected. Your nutrition goals remain unchanged.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("goalError", ex.getMessage());
        }
        return "redirect:/dashboard";
    }
}
