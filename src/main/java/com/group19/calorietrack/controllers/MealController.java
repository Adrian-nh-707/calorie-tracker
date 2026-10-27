package com.group19.calorietrack.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;

@Controller
public class MealController {

    private final MealRepository mealRepository;

    public MealController(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    // it show meals and daily nutrition
    @GetMapping("/meals")
    public String showMeals(
            @RequestParam(required = false) String date,
            Model model) {

        // Use today's date if no dates are selected
        if (date == null || date.isEmpty()) {
            date = LocalDate.now().toString();
        }

        LocalDate selectedDate = LocalDate.parse(date);

        // Get all meals for the selected date
        List<Meal> meals = mealRepository.findByMealDate(selectedDate);

        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        // Calculate daily nutrition totals
        for (Meal meal : meals) {
            totalCalories += meal.getCalories();
            totalProtein += meal.getProtein();
            totalCarbs += meal.getCarbs();
            totalFat += meal.getFat();
        }

        // Send data to HTML
        model.addAttribute("selectedDate", date);
        model.addAttribute("meals", meals);

        model.addAttribute("totalCalories", totalCalories);
        model.addAttribute("totalProtein", totalProtein);
        model.addAttribute("totalCarbs", totalCarbs);
        model.addAttribute("totalFat", totalFat);

        return "meals";
    }

    // Add a new meal
    @PostMapping("/meals/add")
    public String addMeal(
            @RequestParam String name,
            @RequestParam String mealType,
            @RequestParam String mealDate,
            @RequestParam double calories,
            @RequestParam double protein,
            @RequestParam double carbs,
            @RequestParam double fat,
            RedirectAttributes redirectAttributes) {

        LocalDate date = LocalDate.parse(mealDate);

        // Check for invalid input
        if (name.trim().isEmpty()
                || calories < 0
                || protein < 0
                || carbs < 0
                || fat < 0) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Please enter valid meal information."
            );

            return "redirect:/meals?date=" + mealDate;
        }

        // Only allow one Breakfast, Lunch, or Dinner per day
        // Snacks can be added multiple times
        if (!mealType.equals("Snack")) {

            boolean alreadyExists =
                    mealRepository.existsByMealDateAndMealType(
                        date,
                        mealType
                    );

            if (alreadyExists) {

                redirectAttributes.addFlashAttribute(
                    "error",
                    mealType + " has already been logged for this date. "
                    + "Please edit the existing meal instead."
                );

                return "redirect:/meals?date=" + mealDate;
            }
        }

        // Create a new meal
        Meal meal = new Meal(
            name,
            mealType,
            date,
            calories,
            protein,
            carbs,
            fat
        );

        // Save meal to database
        mealRepository.save(meal);

        redirectAttributes.addFlashAttribute(
            "success",
            "Meal added successfully!"
        );

        return "redirect:/meals?date=" + mealDate;
    }
}
