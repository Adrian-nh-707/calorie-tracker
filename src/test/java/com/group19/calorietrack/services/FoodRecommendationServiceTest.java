
package com.group19.calorietrack.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoodRecommendationServiceTest {

    private FoodRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new FoodRecommendationService();
    }
    @Test
    void testZeroRemainingCalories() {
        var results = recommendationService.getRecommendations(0, 40);

        assertTrue(results.isEmpty());
    }

    @Test
    void testNegativeRemainingCalories() {
        var results = recommendationService.getRecommendations(-200, 40);

        assertTrue(results.isEmpty());
    }

    @Test
    void testFoodsStayWithinCalorieLimit() {
        int remainingCalories = 400;

        var results = recommendationService.getRecommendations(
                remainingCalories, 30);

        for (var food : results) {
            assertTrue(food.calories() <= remainingCalories);
        }
    }

    @Test
    void testNoFoodsFitBudget() {
        var results = recommendationService.getRecommendations(100, 30);

        assertTrue(results.isEmpty());
    }

    @Test
    void testReturnsAtMostFiveFoods() {
        var results = recommendationService.getRecommendations(1000, 40);

        assertTrue(results.size() <= 5);
    }

    @Test
    void testRecommendationsAreSortedByScore() {
        var results = recommendationService.getRecommendations(700, 30);

        assertEquals("Chicken Rice Bowl", results.get(0).name());
        assertEquals("Salmon with Rice", results.get(1).name());
    }
    @Test
    void testFoodsAreReturnedWhenBudgetAllows() {
        var results = recommendationService.getRecommendations(200, 20);

        assertEquals(1, results.size());
        assertEquals("Greek Yogurt", results.get(0).name());
    }

    @Test
    void testNoProteinRemaining() {
        var results = recommendationService.getRecommendations(700, 0);

        assertFalse(results.isEmpty());
        assertEquals("Pizza", results.get(0).name());
    }

    @Test
    void testMatchScoreCalculation() {
        var results = recommendationService.getRecommendationDetails(200, 20);

        assertEquals(1, results.size());
        assertEquals("Greek Yogurt", results.get(0).food().name());
        assertEquals(96, results.get(0).matchScore());
        assertEquals("Meets your remaining protein target.",
                     results.get(0).reason());
    }

    @Test
    void testRecommendationDetailsStaySorted() {
        var foods = recommendationService.getRecommendations(700, 30);
        var details = recommendationService.getRecommendationDetails(700, 30);

        assertEquals(foods.size(), details.size());

        for (int i = 0; i < details.size(); i++) {
            assertEquals(foods.get(i), details.get(i).food());

            assertTrue(details.get(i).matchScore() >= 0);
            assertTrue(details.get(i).matchScore() <= 100);
        }

        for (int i = 1; i < details.size(); i++) {
            assertTrue(details.get(i - 1).matchScore()
                    >= details.get(i).matchScore());
        }
    }

    @Test
    void testRecommendationReasonWithoutProteinGoal() {
        var results = recommendationService.getRecommendationDetails(700, 0);

        assertFalse(results.isEmpty());
        assertEquals("Fits your remaining calorie budget.",
                     results.get(0).reason());
    }
}
