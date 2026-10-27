package com.group19.calorietrack.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        //temporary data until meal logging is integrated
        int calorieGoal = 2500;
        int caloriesConsumed = 1800;
        int caloriesRemaining = calorieGoal - caloriesConsumed;

        int protein = 120;
        int carbs = 200;
        int fat = 60;

        //Send data to the Thymeleaf template
        model.addAttribute("calorieGoal", calorieGoal);
        model.addAttribute("caloriesConsumed", caloriesConsumed);
        model.addAttribute("caloriesRemaining", caloriesRemaining);

        model.addAttribute("protein", protein);
        model.addAttribute("carbs", carbs);
        model.addAttribute("fat", fat);

        return "dashboard";
    }
}
