package com.group19.calorietrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalUsers", 0);
        model.addAttribute("totalTrainers", 0);
        model.addAttribute("unassignedUsers", 0);

        return "admin-dashboard";
    }

    @GetMapping("/trainer/dashboard")
    public String trainerDashboard(Model model) {
        model.addAttribute("assignedUserCount", 0);

        return "trainer-dashboard";
    }
}