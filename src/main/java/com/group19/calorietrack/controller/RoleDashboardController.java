package com.group19.calorietrack.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.dto.NutritionProgress;
import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Controller
public class RoleDashboardController {

    private final UserRepository userRepository;
    private final MealRepository mealRepository;

    public RoleDashboardController(
            UserRepository userRepository,
            MealRepository mealRepository) {
        this.userRepository = userRepository;
        this.mealRepository = mealRepository;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long totalUsers = userRepository.countByRole(UserRole.USER);
        long totalTrainers = userRepository.countByRole(UserRole.TRAINER);
        long unassignedUsers
                = userRepository.countByRoleAndAssignedTrainerIsNull(UserRole.USER);

        List<AppUser> users = userRepository.findAll();
        List<AppUser> trainers = userRepository.findByRole(UserRole.TRAINER);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalTrainers", totalTrainers);
        model.addAttribute("unassignedUsers", unassignedUsers);
        model.addAttribute("users", users);
        model.addAttribute("trainers", trainers);

        return "admin-dashboard";
    }

    @PostMapping("/admin/assign-trainer")
    public String assignTrainer(
            @RequestParam Long userId,
            @RequestParam Long trainerId,
            RedirectAttributes redirectAttributes) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(()
                        -> new IllegalArgumentException("User account not found."));

        AppUser trainer = userRepository.findById(trainerId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Trainer account not found."));

        if (user.getRole() != UserRole.USER) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Only regular users can be assigned to trainers.");
            return "redirect:/admin/dashboard";
        }

        if (trainer.getRole() != UserRole.TRAINER) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Please select a valid trainer account.");
            return "redirect:/admin/dashboard";
        }

        user.setAssignedTrainer(trainer);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Trainer assigned successfully.");

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/trainer/dashboard")
    public String trainerDashboard(
            Authentication authentication,
            Model model,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Use today's date if no date was selected
        if (date == null) {
            date = LocalDate.now();
        }

        // Find the currently logged-in trainer
        String email = authentication.getName();

        AppUser trainer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in trainer account was not found."));

        if (trainer.getRole() != UserRole.TRAINER) {
            throw new IllegalStateException(
                    "The logged-in account does not have the trainer role.");
        }

        // Retrieve users assigned to this trainer
        List<AppUser> assignedUsers =
                userRepository.findByAssignedTrainer_IdAndRole(
                        trainer.getId(), UserRole.USER);

        // Create an empty nutrition summary for every assigned user
        Map<Long, NutritionProgress> progressMap = new HashMap<>();

        for (AppUser user : assignedUsers) {
            progressMap.put(user.getId(), new NutritionProgress(user));
        }

        // Retrieve meals for assigned users on the selected date
        if (!assignedUsers.isEmpty()) {

            List<Long> userIds = assignedUsers.stream()
                    .map(AppUser::getId)
                    .collect(Collectors.toList());

            List<Meal> meals =
                    mealRepository.findByUserIdsAndMealDate(userIds, date);

            // Add each meal to the corresponding user's nutrition totals
            for (Meal meal : meals) {
                Long userId = meal.getUser().getId();

                NutritionProgress progress = progressMap.get(userId);

                if (progress != null) {
                    progress.addMeal(
                            meal.getCalories(),
                            meal.getProtein(),
                            meal.getCarbs(),
                            meal.getFat()
                    );
                }
            }
        }

        // Convert the nutrition summaries into a list for the dashboard
        List<NutritionProgress> nutritionProgress =
                new ArrayList<>(progressMap.values());

        model.addAttribute("assignedUserCount", assignedUsers.size());
        model.addAttribute("assignedUsers", assignedUsers);
        model.addAttribute("nutritionProgress", nutritionProgress);
        model.addAttribute("selectedDate", date);

        return "trainer-dashboard";
    }
}
