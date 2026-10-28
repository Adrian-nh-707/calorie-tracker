package com.group19.calorietrack.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Show meals and daily nutrition
    @GetMapping("/meals")
    public String showMeals(
            @RequestParam(required = false) String date,
            Model model) {

        // Use today's date if none is selected
        if (date == null || date.isEmpty()) {
            date = LocalDate.now().toString();
        }

        LocalDate selectedDate = LocalDate.parse(date);

        // Get meals for this date
        List<Meal> meals = mealRepository.findByMealDate(selectedDate);

        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        // Add up the calories and macros
        for (Meal meal : meals) {
            totalCalories += meal.getCalories();
            totalProtein += meal.getProtein();
            totalCarbs += meal.getCarbs();
            totalFat += meal.getFat();
        }

        // Pass everything to the page
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

        // Check if the input is valid
        if (name.trim().isEmpty()
                || !Double.isFinite(calories) || calories < 0
                || !Double.isFinite(protein) || protein < 0
                || !Double.isFinite(carbs) || carbs < 0
                || !Double.isFinite(fat) || fat < 0
                || (!mealType.equals("Breakfast")
                    && !mealType.equals("Lunch")
                    && !mealType.equals("Dinner")
                    && !mealType.equals("Snack"))) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Please enter valid meal information."
            );

            return "redirect:/meals?date=" + mealDate;
        }

        // Only one breakfast, lunch or dinner per day
        // Snacks can be added more than once
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

        // Create and save the meal
        Meal meal = new Meal(
            name,
            mealType,
            date,
            calories,
            protein,
            carbs,
            fat
        );

        mealRepository.save(meal);

        redirectAttributes.addFlashAttribute(
            "success",
            "Meal added successfully!"
        );

        return "redirect:/meals?date=" + mealDate;
    }

    // Open the edit page
    @GetMapping("/meals/edit/{id}")
    public String showEditMeal(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Meal meal = mealRepository.findById(id).orElse(null);

        // Make sure the meal exists
        if (meal == null) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Meal not found."
            );

            return "redirect:/meals";
        }

        model.addAttribute("meal", meal);

        return "edit-meal";
    }

    // Save the edited meal
    @PostMapping("/meals/update/{id}")
    public String updateMeal(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String mealType,
            @RequestParam String mealDate,
            @RequestParam double calories,
            @RequestParam double protein,
            @RequestParam double carbs,
            @RequestParam double fat,
            RedirectAttributes redirectAttributes) {

        // Find the meal we want to edit
        Meal meal = mealRepository.findById(id).orElse(null);

        if (meal == null) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Meal not found."
            );

            return "redirect:/meals";
        }

        LocalDate date = LocalDate.parse(mealDate);

        // Check the new values
        if (name.trim().isEmpty()
                || !Double.isFinite(calories) || calories < 0
                || !Double.isFinite(protein) || protein < 0
                || !Double.isFinite(carbs) || carbs < 0
                || !Double.isFinite(fat) || fat < 0
                || (!mealType.equals("Breakfast")
                    && !mealType.equals("Lunch")
                    && !mealType.equals("Dinner")
                    && !mealType.equals("Snack"))) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Please enter valid meal information."
            );

            return "redirect:/meals/edit/" + id;
        }

        // Check if another meal already uses this type
        // Don't count the meal we're editing
        if (!mealType.equals("Snack")) {

            boolean alreadyExists =
                    mealRepository.existsByMealDateAndMealTypeAndIdNot(
                        date,
                        mealType,
                        id
                    );

            if (alreadyExists) {

                redirectAttributes.addFlashAttribute(
                    "error",
                    mealType + " already exists on this date."
                );

                return "redirect:/meals/edit/" + id;
            }
        }

        // Update the old meal with the new values
        meal.setName(name);
        meal.setMealType(mealType);
        meal.setMealDate(date);
        meal.setCalories(calories);
        meal.setProtein(protein);
        meal.setCarbs(carbs);
        meal.setFat(fat);

        mealRepository.save(meal);

        redirectAttributes.addFlashAttribute(
            "success",
            "Meal updated successfully!"
        );

        return "redirect:/meals?date=" + mealDate;
    }

    // Delete a meal
    @PostMapping("/meals/delete/{id}")
    public String deleteMeal(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Meal meal = mealRepository.findById(id).orElse(null);

        if (meal == null) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Meal not found."
            );

            return "redirect:/meals";
        }

        // Keep the date so we can go back to it
        String date = meal.getMealDate().toString();

        mealRepository.delete(meal);

        redirectAttributes.addFlashAttribute(
            "success",
            "Meal deleted successfully!"
        );

        return "redirect:/meals?date=" + date;
    }
}
