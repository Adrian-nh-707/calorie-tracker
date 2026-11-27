
package com.group19.calorietrack.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.dto.NutritionProgress;
import com.group19.calorietrack.messaging.MessageService;
import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.services.TrainerGoalRecommendationService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RoleDashboardController {

    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final TrainerGoalRecommendationService recommendationService;
    private final MessageService messageService;

    public RoleDashboardController(
            UserRepository userRepository,
            MealRepository mealRepository,
            MessageService messageService,
            TrainerGoalRecommendationService recommendationService) {

        this.userRepository = userRepository;
        this.mealRepository = mealRepository;
        this.messageService = messageService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long totalUsers = userRepository.countByRole(UserRole.USER);
        long totalTrainers = userRepository.countByRole(UserRole.TRAINER);
        long unassignedUsers =
                userRepository.countByRoleAndAssignedTrainerIsNull(UserRole.USER);

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
                .orElseThrow(() ->
                        new IllegalArgumentException("User account not found."));

        AppUser trainer = userRepository.findById(trainerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Trainer account not found."));

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
            HttpServletRequest request,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Initialize CSRF before rendering the dashboard.
        // Prevents session creation after the response is committed.
        CsrfToken csrfToken = (CsrfToken)
                request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            csrfToken.getToken();
        }

        if (date == null) {
            date = LocalDate.now();
        }

        String email = authentication.getName();

        AppUser trainer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in trainer account was not found."));

        if (trainer.getRole() != UserRole.TRAINER) {
            throw new IllegalStateException(
                    "The logged-in account does not have the trainer role.");
        }

        List<AppUser> assignedUsers =
                userRepository.findByAssignedTrainer_IdAndRole(
                        trainer.getId(), UserRole.USER);

        Map<Long, NutritionProgress> progressMap = new HashMap<>();

        for (AppUser user : assignedUsers) {
            progressMap.put(user.getId(), new NutritionProgress(user));
        }

        if (!assignedUsers.isEmpty()) {

            List<Long> userIds = assignedUsers.stream()
                    .map(AppUser::getId)
                    .collect(Collectors.toList());

            List<Meal> meals =
                    mealRepository.findByUserIdsAndMealDate(userIds, date);

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

        List<NutritionProgress> nutritionProgress =
                new ArrayList<>(progressMap.values());

        model.addAttribute("assignedUserCount", assignedUsers.size());
        model.addAttribute("assignedUsers", assignedUsers);
        model.addAttribute("nutritionProgress", nutritionProgress);
        model.addAttribute("selectedDate", date);
        model.addAttribute("unreadCount", messageService.getUnreadCount(email));

        // Iteration 3: Display sent recommendations and their statuses.
        model.addAttribute(
                "sentGoalRecommendations",
                recommendationService.getRecommendationsForTrainer(email)
        );

        return "trainer-dashboard";
    }

    @GetMapping("/home")
    public String redirectToDashboard(Authentication authentication) {

        boolean isTrainer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAINER"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isTrainer) {
            return "redirect:/trainer/dashboard";
        }

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/trainer/recommendations")
    public String createGoalRecommendation(
            Authentication authentication,
            @RequestParam Long userId,
            @RequestParam Integer calorieGoal,
            @RequestParam Integer proteinGoal,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        try {
            recommendationService.createRecommendation(
                    authentication.getName(),
                    userId,
                    calorieGoal,
                    proteinGoal,
                    note
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Goal recommendation sent successfully."
            );

        } catch (IllegalArgumentException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/trainer/dashboard";
    }
}
