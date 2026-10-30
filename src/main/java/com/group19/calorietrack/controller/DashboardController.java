
package com.group19.calorietrack.controller;

import com.group19.calorietrack.services.FoodRecommendationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final FoodRecommendationService recommendationService;

    public DashboardController(FoodRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {

        //Get the calorie goal stored in the session
        Integer calorieGoal = (Integer) session.getAttribute("calorieGoal");
        //Set a default goal if the user has not chosen one
        if (calorieGoal == null) {
            calorieGoal = 2500;
        }
        //Temporary data sample
        int caloriesConsumed = 1800;
        int protein = 120;
        int carbs = 200;
        int fat = 60;

        int proteinGoal = 150;

        // Calculate remaining calories and protein
        int caloriesRemaining = calorieGoal - caloriesConsumed;
        int proteinRemaining = proteinGoal - protein;

        // Get food recommendations
        var recommendations = recommendationService.getRecommendations(
                caloriesRemaining, proteinRemaining);

        // Send data to the dashboard
        model.addAttribute("calorieGoal", calorieGoal);
        model.addAttribute("caloriesConsumed", caloriesConsumed);
        model.addAttribute("caloriesRemaining", caloriesRemaining);

        model.addAttribute("protein", protein);
        model.addAttribute("carbs", carbs);
        model.addAttribute("fat", fat);

        model.addAttribute("proteinGoal", proteinGoal);
        model.addAttribute("proteinRemaining", proteinRemaining);
        model.addAttribute("recommendations", recommendations);

        return "dashboard";
    }

    @PostMapping("/dashboard/goal")
    public String updateGoal(@RequestParam("calorieGoal") int calorieGoal, HttpSession session) {

            //Only accept positive calorie goals
         if (calorieGoal >= 1 && calorieGoal <= 10000) { //Reject unrealistic goals
            session.setAttribute("calorieGoal", calorieGoal);
        }

        return "redirect:/dashboard";
    }
}
