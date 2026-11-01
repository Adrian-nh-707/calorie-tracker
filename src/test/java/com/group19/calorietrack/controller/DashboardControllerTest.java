
package com.group19.calorietrack.controller;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.services.FoodRecommendationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardControllerTest {

    private DashboardController controller;
    private MealRepository mealRepository;
    private FoodRecommendationService recommendationService;
    private ExtendedModelMap model;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mealRepository = mock(MealRepository.class);
        recommendationService = mock(FoodRecommendationService.class);

        controller = new DashboardController(
                recommendationService, mealRepository);

        model = new ExtendedModelMap();
        session = new MockHttpSession();
    }

    @Test
    void testDashboardWithNoMeals() {
        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of());

        String page = controller.showDashboard(model, session);

        assertEquals("dashboard", page);
        assertEquals(2500, model.get("calorieGoal"));
        assertEquals(0.0, model.get("caloriesConsumed"));
        assertEquals(2500.0, model.get("caloriesRemaining"));
        assertEquals(0, model.get("mealCount"));
        assertEquals(true, model.get("noMeals"));

        verify(recommendationService).getRecommendationDetails(2500, 150);
    }

    @Test
    void testDashboardWithMultipleMeals() {
        LocalDate today = LocalDate.now();

        Meal breakfast = new Meal(
                "Egg Sandwich", "Breakfast", today,
                500, 30, 50, 10);

        Meal lunch = new Meal(
                "Chicken Rice", "Lunch", today,
                300, 20, 30, 15);

        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of(breakfast, lunch));

        controller.showDashboard(model, session);

        assertEquals(800.0, model.get("caloriesConsumed"));
        assertEquals(50.0, model.get("protein"));
        assertEquals(80.0, model.get("carbs"));
        assertEquals(25.0, model.get("fat"));
        assertEquals(1700.0, model.get("caloriesRemaining"));
        assertEquals(2, model.get("mealCount"));
        assertEquals(false, model.get("noMeals"));

        verify(recommendationService).getRecommendationDetails(1700, 100);
    }

    @Test
    void testCustomCalorieGoal() {
        LocalDate today = LocalDate.now();

        Meal lunch = new Meal(
                "Chicken Rice", "Lunch", today,
                800, 20, 60, 15);

        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of(lunch));

        session.setAttribute("calorieGoal", 3000);

        controller.showDashboard(model, session);

        assertEquals(3000, model.get("calorieGoal"));
        assertEquals(2200.0, model.get("caloriesRemaining"));

        verify(recommendationService).getRecommendationDetails(2200, 130);
    }

    @Test
    void testDecimalNutritionValues() {
        LocalDate today = LocalDate.now();

        Meal snack = new Meal(
                "Yogurt", "Snack", today,
                100.5, 1.2, 10.5, 2.5);

        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of(snack));

        controller.showDashboard(model, session);

        assertEquals(100.5, model.get("caloriesConsumed"));
        assertEquals(1.2, (double) model.get("protein"), 0.001);
        assertEquals(2399.5, model.get("caloriesRemaining"));

        verify(recommendationService).getRecommendationDetails(2399, 149);
    }

    @Test
    void testExceededCalorieGoal() {
        LocalDate today = LocalDate.now();

        Meal dinner = new Meal(
                "Large Dinner", "Dinner", today,
                2700, 160, 250, 80);

        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of(dinner));

        controller.showDashboard(model, session);

        assertEquals(-200.0, model.get("caloriesRemaining"));

        verify(recommendationService).getRecommendationDetails(0, 0);
    }

    @Test
    void testUpdateCalorieGoal() {
        String result = controller.updateGoal(3000, session);

        assertEquals("redirect:/dashboard", result);
        assertEquals(3000, session.getAttribute("calorieGoal"));
    }

    @Test
    void testInvalidCalorieGoals() {
        session.setAttribute("calorieGoal", 2500);

        controller.updateGoal(0, session);
        controller.updateGoal(10001, session);

        assertEquals(2500, session.getAttribute("calorieGoal"));
    }

    @Test
    void testRecommendationsPassedToDashboard() {
        when(mealRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(List.of());

        FoodRecommendationService.Food food =
                new FoodRecommendationService.Food(
                        "Chicken Rice Bowl", 600, 42);

        FoodRecommendationService.Recommendation recommendation =
                new FoodRecommendationService.Recommendation(
                        food, 85, "Fits your calorie budget.");

        var recommendations = List.of(recommendation);

        when(recommendationService.getRecommendationDetails(2500, 150))
                .thenReturn(recommendations);

        controller.showDashboard(model, session);

        assertEquals(recommendations, model.get("recommendations"));
    }
}
