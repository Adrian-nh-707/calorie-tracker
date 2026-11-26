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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.dto.NutritionProgress;
import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.messaging.MessageService;
import com.group19.calorietrack.services.TrainerGoalRecommendationService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;
import com.group19.calorietrack.services.AdminAccountManagementService;

@Controller
public class RoleDashboardController {

    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final TrainerGoalRecommendationService recommendationService;
    private final MessageService messageService;
    private final AdminAccountManagementService accountManagementService;

    public RoleDashboardController(
            UserRepository userRepository,
            MealRepository mealRepository,
            MessageService messageService,
            TrainerGoalRecommendationService recommendationService,
            AdminAccountManagementService accountManagementService) {

        this.userRepository = userRepository;
        this.mealRepository = mealRepository;
        this.messageService = messageService;
        this.recommendationService = recommendationService;
        this.accountManagementService = accountManagementService;
    }

        @GetMapping("/admin/dashboard")
        public String adminDashboard(Model model) {

        long totalUsers = userRepository.countByRole(UserRole.USER);
        long totalTrainers = userRepository.countByRole(UserRole.TRAINER);
        long unassignedUsers = userRepository.countByRoleAndAssignedTrainerIsNull(UserRole.USER);
        long assignedUserCount = totalUsers - unassignedUsers;

        long activeAccounts = userRepository.countByEnabled(true);
        long inactiveAccounts = userRepository.countByEnabled(false);
        long totalAccounts = userRepository.count();

        List<AppUser> users = userRepository.findAll();
        List<AppUser> trainers = userRepository.findByRole(UserRole.TRAINER);
        Map<Long, Long> trainerWorkloads = new HashMap<>();

        for (AppUser trainer : trainers) {
                long assignedCount =
                        userRepository.countByAssignedTrainer_IdAndRole(
                                trainer.getId(),
                                UserRole.USER
                        );

                trainerWorkloads.put(trainer.getId(), assignedCount);
        }

        double averageUsersPerTrainer = totalTrainers > 0
                ? (double) assignedUserCount / totalTrainers
                : 0.0;

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalTrainers", totalTrainers);
        model.addAttribute("unassignedUsers", unassignedUsers);
        model.addAttribute("assignedUserCount", assignedUserCount);
        model.addAttribute("averageUsersPerTrainer", averageUsersPerTrainer);

        model.addAttribute("users", users);
        model.addAttribute("trainers", trainers);
        model.addAttribute("trainerWorkloads", trainerWorkloads);

        model.addAttribute("activeAccounts", activeAccounts);
        model.addAttribute("inactiveAccounts", inactiveAccounts);
        model.addAttribute("totalAccounts", totalAccounts);

        return "admin-dashboard";
    }

    @PostMapping("/admin/assign-trainer")
    public String assignTrainer(
            @RequestParam Long userId,
            @RequestParam(required = false) Long trainerId,
            RedirectAttributes redirectAttributes) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User account not found."));

        if (user.getRole() != UserRole.USER) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only regular users can be assigned to trainers.");
            return "redirect:/admin/dashboard";
        }

        if (trainerId == null) {
            user.setAssignedTrainer(null);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Trainer assignment removed successfully.");
            return "redirect:/admin/dashboard";
        }

        AppUser trainer = userRepository.findById(trainerId)
            .orElseThrow(() ->
                        new IllegalArgumentException("Trainer account not found."));

        if (trainer.getRole() != UserRole.TRAINER) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid trainer account.");
            return "redirect:/admin/dashboard";
        }

        user.setAssignedTrainer(trainer);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Trainer assigned successfully.");

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/change-role")
        public String changeAccountRole(
                @RequestParam Long userId,
                @RequestParam UserRole newRole,
                RedirectAttributes redirectAttributes) {

        try {
                accountManagementService.changeUserRole(userId, newRole);
                redirectAttributes.addFlashAttribute("successMessage", "Account role updated successfully.");

        } catch (IllegalArgumentException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/deactivate-account")
    public String deactivateAccount(
            Authentication authentication,
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {

        try {
            accountManagementService.deactivateAccount(userId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Account deactivated successfully.");

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/reactivate-account")
    public String reactivateAccount(
            Authentication authentication,
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {

        try {
            accountManagementService.reactivateAccount(userId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Account reactivated successfully.");

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/trainer/dashboard")
    public String trainerDashboard(
            Authentication authentication,
            Model model,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        String email = authentication.getName();

        AppUser trainer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in trainer account was not found."));

        if (trainer.getRole() != UserRole.TRAINER) {
            throw new IllegalStateException("The logged-in account does not have the trainer role.");
        }

        List<AppUser> assignedUsers = userRepository.findByAssignedTrainer_IdAndRole(trainer.getId(), UserRole.USER);
        Map<Long, NutritionProgress> progressMap = new HashMap<>();

        for (AppUser user : assignedUsers) {
            progressMap.put(user.getId(), new NutritionProgress(user));
        }

        if (!assignedUsers.isEmpty()) {

            List<Long> userIds = assignedUsers.stream()
                    .map(AppUser::getId)
                    .collect(Collectors.toList());

            List<Meal> meals = mealRepository.findByUserIdsAndMealDate(userIds, date);

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