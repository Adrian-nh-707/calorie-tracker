package com.group19.calorietrack.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Controller
public class RoleDashboardController {

    private final UserRepository userRepository;

    public RoleDashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
            Model model) {

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

        model.addAttribute("assignedUserCount", assignedUsers.size());
        model.addAttribute("assignedUsers", assignedUsers);

        return "trainer-dashboard";
    }
}