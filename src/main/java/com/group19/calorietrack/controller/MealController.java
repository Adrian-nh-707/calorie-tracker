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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;

@Controller
public class MealController {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;

    public MealController(MealRepository mealRepository,
                          UserRepository userRepository) {
        this.mealRepository = mealRepository;
        this.userRepository = userRepository;
    }

    // Get the logged-in user
    private AppUser getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Please log in."
            );
        }

        return userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User not found."
            ));
    }

    // Get a meal belonging to the logged-in user
    private Meal getOwnedMeal(Long id, Long userId) {
        return mealRepository.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Meal not found."
            ));
    }

    // Show meals and daily nutrition
    @GetMapping("/meals")
    public String showMeals(
            @RequestParam(required = false) String date,
            Model model,
            Principal principal) {

        AppUser user = getCurrentUser(principal);

        if (date == null || date.isEmpty()) {
            date = LocalDate.now().toString();
        }

        LocalDate selectedDate = LocalDate.parse(date);

        // Only show this user's meals
        List<Meal> meals = mealRepository.findByUser_IdAndMealDate(
            user.getId(),
            selectedDate
        );

        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        for (Meal meal : meals) {
            totalCalories += meal.getCalories();
            totalProtein += meal.getProtein();
            totalCarbs += meal.getCarbs();
            totalFat += meal.getFat();
        }

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
            Principal principal,
            RedirectAttributes redirectAttributes) {

        AppUser user = getCurrentUser(principal);
        LocalDate date = LocalDate.parse(mealDate);

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

        // Check duplicates for this user only
        if (!mealType.equals("Snack")) {
            boolean alreadyExists =
                mealRepository.existsByUser_IdAndMealDateAndMealType(
                    user.getId(),
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

        Meal meal = new Meal(
            name, mealType, date,
            calories, protein, carbs, fat
        );

        // Assign ownership before saving
        meal.setUser(user);
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
            Principal principal) {

        AppUser user = getCurrentUser(principal);

        Meal meal = getOwnedMeal(id, user.getId());

        model.addAttribute("meal", meal);

        return "edit-meal";
    }

    // Update an existing meal
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
            Principal principal,
            RedirectAttributes redirectAttributes) {

        AppUser user = getCurrentUser(principal);

        // Only the owner can update this meal
        Meal meal = getOwnedMeal(id, user.getId());

        LocalDate date = LocalDate.parse(mealDate);

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

        // Check duplicates for this user only
        if (!mealType.equals("Snack")) {
            boolean alreadyExists =
                mealRepository
                    .existsByUser_IdAndMealDateAndMealTypeAndIdNot(
                        user.getId(),
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
            Principal principal,
            RedirectAttributes redirectAttributes) {

        AppUser user = getCurrentUser(principal);

        // Only the owner can delete this meal
        Meal meal = getOwnedMeal(id, user.getId());

        String date = meal.getMealDate().toString();

        mealRepository.delete(meal);

        redirectAttributes.addFlashAttribute(
            "success",
            "Meal deleted successfully!"
        );

        return "redirect:/meals?date=" + date;
    }
}
