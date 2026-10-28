package com.group19.calorietrack.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {
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
    @PostMapping("/dashboard/goal")
    public String updateGoal(@RequestParam("calorieGoal") int calorieGoal, HttpSession session) {

    //Only accept positive calorie goals
    if (calorieGoal >= 1 && calorieGoal <= 10000) { //Reject unrealistic goals
        session.setAttribute("calorieGoal", calorieGoal);
    }

    return "redirect:/dashboard";
}
}
