
package com.group19.calorietrack.controller;

import com.group19.calorietrack.services.FoodRecommendationService;
import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final FoodRecommendationService recommendationService;
    private final MealRepository mealRepository;

    public DashboardController(FoodRecommendationService recommendationService,
                               MealRepository mealRepository) {
        this.recommendationService = recommendationService;
        this.mealRepository = mealRepository;
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
        //Replace sample values with totals from today's logged meals
        double caloriesConsumed = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;

        LocalDate today = LocalDate.now();

        //Get meals logged for today
        List<Meal> meals = mealRepository.findByMealDate(today);

        //Add up the calories and macros from each meal
        for (Meal meal : meals) {
            caloriesConsumed += meal.getCalories();
            protein += meal.getProtein();
            carbs += meal.getCarbs();
            fat += meal.getFat();
        }

        int proteinGoal = 150;

        // Calculate remaining calories and protein
        double caloriesRemaining = calorieGoal - caloriesConsumed;
        double proteinRemaining = proteinGoal - protein;

        //Convert remaining values for the recommendation service
        int remainingCaloriesForRecommendations =
                (int) Math.max(0, Math.floor(caloriesRemaining));

        int remainingProteinForRecommendations =
                (int) Math.max(0, Math.ceil(proteinRemaining));

        // Get food recommendations
        var recommendations = recommendationService.getRecommendations(
                remainingCaloriesForRecommendations,
                remainingProteinForRecommendations);

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
        model.addAttribute("selectedDate", today);

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
