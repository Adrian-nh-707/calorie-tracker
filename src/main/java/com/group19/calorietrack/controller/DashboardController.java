
package com.group19.calorietrack.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.services.FoodRecommendationService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    private final FoodRecommendationService recommendationService;
    private final MealRepository mealRepository;
    private final UserRepository userRepository;

    public DashboardController(
            FoodRecommendationService recommendationService,
            MealRepository mealRepository,
            UserRepository userRepository) {

        this.recommendationService = recommendationService;
        this.mealRepository = mealRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(
            Model model,
            HttpSession session,
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Please log in."
            );
        }

        // Get logged-in user
        AppUser user = userRepository
            .findByEmail(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User not found."
            ));

        // Get calorie goal from session
        Integer calorieGoal =
            (Integer) session.getAttribute("calorieGoal");

        if (calorieGoal == null) {
            calorieGoal = 2500;
        }

        double caloriesConsumed = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;

        LocalDate today = LocalDate.now();

        // Get only this user's meals
        List<Meal> meals =
            mealRepository.findByUser_IdAndMealDate(
                user.getId(),
                today
            );

        // Calculate this user's nutrition totals
        for (Meal meal : meals) {
            caloriesConsumed += meal.getCalories();
            protein += meal.getProtein();
            carbs += meal.getCarbs();
            fat += meal.getFat();
        }

        int proteinGoal = 150;

        double caloriesRemaining =
            calorieGoal - caloriesConsumed;

        double proteinRemaining =
            proteinGoal - protein;

        int remainingCaloriesForRecommendations =
            (int) Math.max(0, Math.floor(caloriesRemaining));

        int remainingProteinForRecommendations =
            (int) Math.max(0, Math.ceil(proteinRemaining));

        var recommendations =
            recommendationService.getRecommendationDetails(
                remainingCaloriesForRecommendations,
                remainingProteinForRecommendations
            );

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

        model.addAttribute("mealCount", meals.size());
        model.addAttribute("noMeals", meals.isEmpty());

        return "dashboard";
    }

    @PostMapping("/dashboard/goal")
    public String updateGoal(
            @RequestParam("calorieGoal") int calorieGoal,
            HttpSession session) {

        if (calorieGoal >= 1 && calorieGoal <= 10000) {
            session.setAttribute("calorieGoal", calorieGoal);
        }

        return "redirect:/dashboard";
    }
}
