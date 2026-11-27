package com.group19.calorietrack.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.services.FoodRecommendationService;
import com.group19.calorietrack.services.WeeklyNutritionService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;

import com.group19.calorietrack.messaging.MessageService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    private final FoodRecommendationService recommendationService;
    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final WeeklyNutritionService weeklyNutritionService;

    private final MessageService messageService;

    public DashboardController(
            FoodRecommendationService recommendationService,
            MealRepository mealRepository,
            UserRepository userRepository,
            MessageService messageService,
            WeeklyNutritionService weeklyNutritionService) {

        this.recommendationService = recommendationService;
        this.mealRepository = mealRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.weeklyNutritionService = weeklyNutritionService;
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
            model.addAttribute(
                "unreadCount",
                messageService.getUnreadCount(principal.getName())
            );

        // Use the authenticated user's persisted target (not a session-only goal).
        Integer calorieGoal = user.getDailyCalorieGoal();

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

        int proteinGoal = user.getDailyProteinGoal() == null ? 150 : user.getDailyProteinGoal();

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

        // Get the last seven days of nutrition data
        var weeklySummary = weeklyNutritionService.getWeeklySummary(
                user.getId(),
                today,
                calorieGoal
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

        // Send weekly nutrition information to the dashboard
        model.addAttribute("weeklySummary", weeklySummary);

        return "dashboard";
    }

    @GetMapping("/dashboard/day/{date}")
    public String showDayDetail(
            Model model,
            HttpSession session,
            Principal principal,
            @PathVariable("date") java.time.LocalDate date) {

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

        // Use the authenticated user's persisted target (not a session-only goal).
        Integer calorieGoal = user.getDailyCalorieGoal();

        if (calorieGoal == null) {
            calorieGoal = 2500;
        }

        // Get day details using the service
        var dayDetails = weeklyNutritionService.getDayDetails(
            user.getId(),
            date,
            calorieGoal
        );

        model.addAttribute("dayDetails", dayDetails);
        model.addAttribute("calorieGoal", calorieGoal);
        model.addAttribute("unreadCount",
            messageService.getUnreadCount(principal.getName()));

        return "day-detail";
    }

    @PostMapping("/dashboard/goal")
    public String updateGoal(
            @RequestParam("calorieGoal") int calorieGoal,
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in.");
        }
        AppUser user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        if (calorieGoal >= 1 && calorieGoal <= 10000) {
            user.setDailyCalorieGoal(calorieGoal);
            userRepository.save(user);
        }

        return "redirect:/dashboard";
    }
}
