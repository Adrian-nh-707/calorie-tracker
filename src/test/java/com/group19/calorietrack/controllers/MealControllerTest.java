package com.group19.calorietrack.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;

class MealControllerTest {

    private MealRepository mealRepository;
    private MealController mealController;
    private RedirectAttributes redirectAttributes;

    @BeforeEach
    void setUp() {

        // Set up a fake repository for each test
        mealRepository = mock(MealRepository.class);
        redirectAttributes = mock(RedirectAttributes.class);

        mealController = new MealController(mealRepository);
    }

    @Test
    void testAddMeal() {

        // Add a new breakfast
        String result = mealController.addMeal(
            "Egg Sandwich",
            "Breakfast",
            "2026-10-27",
            320,
            18,
            40,
            12,
            redirectAttributes
        );

        // Check if the meal was saved
        verify(mealRepository).save(any(Meal.class));

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testDuplicateBreakfast() {

        LocalDate date = LocalDate.of(2026, 10, 27);

        // Pretend breakfast already exists
        when(mealRepository.existsByMealDateAndMealType(
            date, "Breakfast"
        )).thenReturn(true);

        String result = mealController.addMeal(
            "Toast",
            "Breakfast",
            "2026-10-27",
            200,
            5,
            30,
            6,
            redirectAttributes
        );

        // The second breakfast should not be saved
        verify(mealRepository, never()).save(any(Meal.class));

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testMultipleSnacks() {

        // Add two snacks on the same day
        mealController.addMeal(
            "Banana",
            "Snack",
            "2026-10-27",
            100,
            1,
            27,
            0,
            redirectAttributes
        );

        mealController.addMeal(
            "Apple",
            "Snack",
            "2026-10-27",
            95,
            0,
            25,
            0,
            redirectAttributes
        );

        // Both snacks should be saved
        verify(mealRepository, times(2)).save(any(Meal.class));
    }

    @Test
    void testInvalidMeal() {

        // Calories cannot be negative
        String result = mealController.addMeal(
            "Chicken Rice",
            "Lunch",
            "2026-10-27",
            -100,
            30,
            50,
            10,
            redirectAttributes
        );

        verify(mealRepository, never()).save(any(Meal.class));

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testDailyNutrition() {

        LocalDate date = LocalDate.of(2026, 10, 27);

        Meal breakfast = new Meal(
            "Egg Sandwich",
            "Breakfast",
            date,
            320,
            18,
            40,
            12
        );

        Meal lunch = new Meal(
            "Chicken Rice",
            "Lunch",
            date,
            550,
            35,
            65,
            15
        );

        // Return two meals for this date
        when(mealRepository.findByMealDate(date))
            .thenReturn(List.of(breakfast, lunch));

        Model model = mock(Model.class);

        String result = mealController.showMeals(
            "2026-10-27",
            model
        );

        // Check the daily totals
        verify(model).addAttribute("totalCalories", 870.0);
        verify(model).addAttribute("totalProtein", 53.0);
        verify(model).addAttribute("totalCarbs", 105.0);
        verify(model).addAttribute("totalFat", 27.0);

        assertEquals("meals", result);
    }

    @Test
    void testEditMeal() {

        Meal meal = new Meal(
            "Egg Sandwich",
            "Breakfast",
            LocalDate.of(2026, 10, 27),
            320,
            18,
            40,
            12
        );

        // Find the meal we want to edit
        when(mealRepository.findById(1L))
            .thenReturn(Optional.of(meal));

        String result = mealController.updateMeal(
            1L,
            "Egg Sandwich",
            "Breakfast",
            "2026-10-27",
            400,
            18,
            40,
            12,
            redirectAttributes
        );

        // Check the updated calories
        assertEquals(400, meal.getCalories());

        verify(mealRepository).save(meal);

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testEditDuplicateMeal() {

        LocalDate date = LocalDate.of(2026, 10, 27);

        Meal lunch = new Meal(
            "Chicken Rice",
            "Lunch",
            date,
            550,
            35,
            65,
            15
        );

        when(mealRepository.findById(1L))
            .thenReturn(Optional.of(lunch));

        // Pretend another breakfast already exists
        when(mealRepository.existsByMealDateAndMealTypeAndIdNot(
            date, "Breakfast", 1L
        )).thenReturn(true);

        String result = mealController.updateMeal(
            1L,
            "Chicken Rice",
            "Breakfast",
            "2026-10-27",
            550,
            35,
            65,
            15,
            redirectAttributes
        );

        // Don't save if the meal type already exists
        verify(mealRepository, never()).save(any(Meal.class));

        assertEquals(
            "redirect:/meals/edit/1",
            result
        );

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );
    }

    @Test
    void testOpenEditPage() {

        Meal meal = new Meal(
            "Banana",
            "Snack",
            LocalDate.of(2026, 10, 27),
            100,
            1,
            27,
            0
        );

        when(mealRepository.findById(1L))
            .thenReturn(Optional.of(meal));

        Model model = mock(Model.class);

        String result = mealController.showEditMeal(
            1L,
            model,
            redirectAttributes
        );

        // Check if the meal is sent to the edit page
        verify(model).addAttribute("meal", meal);

        assertEquals("edit-meal", result);
    }

    @Test
    void testDeleteMeal() {

        Meal meal = new Meal(
            "Banana",
            "Snack",
            LocalDate.of(2026, 10, 27),
            100,
            1,
            27,
            0
        );

        when(mealRepository.findById(1L))
            .thenReturn(Optional.of(meal));

        String result = mealController.deleteMeal(
            1L,
            redirectAttributes
        );

        // Check if the meal was deleted
        verify(mealRepository).delete(meal);

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testDeleteMissingMeal() {

        // Pretend the meal does not exist
        when(mealRepository.findById(99L))
            .thenReturn(Optional.empty());

        String result = mealController.deleteMeal(
            99L,
            redirectAttributes
        );

        // Nothing should be deleted
        verify(mealRepository, never()).delete(any(Meal.class));

        assertEquals("redirect:/meals", result);

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );
    }

    @Test
    void testEmptyMealName() {

        // Try adding a meal with an empty name
        String result = mealController.addMeal(
            "   ",
            "Breakfast",
            "2026-10-27",
            320,
            18,
            40,
            12,
            redirectAttributes
        );

        // The meal should not be saved
        verify(mealRepository, never()).save(any(Meal.class));

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testInvalidNutrition() {

        // Try adding a meal with negative protein
        String result = mealController.addMeal(
            "Chicken Rice",
            "Lunch",
            "2026-10-27",
            550,
            -10,
            65,
            15,
            redirectAttributes
        );

        verify(mealRepository, never()).save(any(Meal.class));

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testEditMissingMeal() {

        // Pretend the meal does not exist
        when(mealRepository.findById(99L))
            .thenReturn(Optional.empty());

        String result = mealController.updateMeal(
            99L,
            "Chicken Rice",
            "Lunch",
            "2026-10-27",
            550,
            35,
            65,
            15,
            redirectAttributes
        );

        // Nothing should be saved
        verify(mealRepository, never()).save(any(Meal.class));

        verify(redirectAttributes).addFlashAttribute(
            eq("error"),
            anyString()
        );

        assertEquals("redirect:/meals", result);
    }

    @Test
    void testEditSameMealType() {

        LocalDate date = LocalDate.of(2026, 10, 27);

        Meal meal = new Meal(
            "Egg Sandwich",
            "Breakfast",
            date,
            320,
            18,
            40,
            12
        );

        when(mealRepository.findById(1L))
            .thenReturn(Optional.of(meal));

        // No other breakfast exists
        when(mealRepository.existsByMealDateAndMealTypeAndIdNot(
            date, "Breakfast", 1L
        )).thenReturn(false);

        String result = mealController.updateMeal(
            1L,
            "Egg Sandwich",
            "Breakfast",
            "2026-10-27",
            400,
            18,
            40,
            12,
            redirectAttributes
        );

        // Editing the same breakfast should work
        verify(mealRepository).save(meal);

        verify(mealRepository).existsByMealDateAndMealTypeAndIdNot(
            date, "Breakfast", 1L
        );

        assertEquals(400, meal.getCalories());

        assertEquals(
            "redirect:/meals?date=2026-10-27",
            result
        );
    }

    @Test
    void testEmptyMealHistory() {

        LocalDate date = LocalDate.of(2026, 10, 27);

        // No meals were logged for this date
        when(mealRepository.findByMealDate(date))
            .thenReturn(List.of());

        Model model = mock(Model.class);

        String result = mealController.showMeals(
            "2026-10-27",
            model
        );

        // All daily totals should be zero
        verify(model).addAttribute("totalCalories", 0.0);
        verify(model).addAttribute("totalProtein", 0.0);
        verify(model).addAttribute("totalCarbs", 0.0);
        verify(model).addAttribute("totalFat", 0.0);

        verify(model).addAttribute("meals", List.of());

        assertEquals("meals", result);
    }
}

