
package com.group19.calorietrack.services;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class FoodRecommendationService {

    // Temporary food data until database integration
    public record Food(String name, int calories, int protein) {}

    public record Recommendation(Food food, int matchScore, String reason) {}

    private final List<Food> foods = List.of(
        new Food("Chicken Rice Bowl", 600, 42),
        new Food("Turkey Sandwich", 450, 30),
        new Food("Greek Yogurt", 180, 20),
        new Food("Oatmeal", 300, 12),
        new Food("Pizza", 620, 18),
        new Food("Salmon with Rice", 550, 40),
        new Food("Protein Shake", 250, 30),
        new Food("Chicken Salad", 350, 35)
    );

    // Get the top 5 foods based on remaining calories and protein
    public List<Food> getRecommendations(int remainingCalories,
                                         int remainingProtein) {

        List<Food> recommendations = new ArrayList<>();

        if (remainingCalories <= 0) {
            return recommendations;
        }

        // Find foods that fit the calorie limit
        for (Food food : foods) {
            if (food.calories() > 0 && food.calories() <= remainingCalories) {
                recommendations.add(food);
            }
        }

        // Sort foods by their recommendation scores
        recommendations.sort((food1, food2) -> {
            double score1 = calculateScore(food1, remainingCalories,
                                           remainingProtein);
            double score2 = calculateScore(food2, remainingCalories,
                                           remainingProtein);

            return Double.compare(score2, score1);
        });

        // Return at most 5 recommendations
        return recommendations.stream()
                .limit(5)
                .toList();
    }

    // Add match scores and explanations to the recommended foods
    public List<Recommendation> getRecommendationDetails(int remainingCalories,int remainingProtein) {

        List<Food> recommendedFoods = getRecommendations(
                remainingCalories, remainingProtein);

        List<Recommendation> details = new ArrayList<>();

        for (Food food : recommendedFoods) {
            double score = calculateScore(
                    food, remainingCalories, remainingProtein);

            int matchScore = (int) Math.round(score * 100);
            matchScore = Math.max(0, Math.min(100, matchScore));

            String reason = getRecommendationReason(
                    food, remainingProtein);

            details.add(new Recommendation(food, matchScore, reason));
        }

        return details;
    }

    // Calculate how well a food matches the user's remaining goals
    private double calculateScore(Food food, int remainingCalories,
                                  int remainingProtein) {

        double calorieScore = 1.0 -
                Math.abs(remainingCalories - food.calories())
                / (double) remainingCalories;

        double proteinScore;

        if (remainingProtein > 0) {
            proteinScore = Math.min(food.protein(), remainingProtein)
                    / (double) remainingProtein;
        } else {
            proteinScore = 0;
        }

        // Calories have 40% weight, protein has 60%
        return 0.4 * calorieScore + 0.6 * proteinScore;
    }

    //Explain why th food was recommended
    private String getRecommendationReason(Food food, int remainingProtein) {

        if (remainingProtein <= 0) {
            return "Fits your remaining calorie budget.";
        }

        if (food.protein() >= remainingProtein) {
            return "Meets your remaining protein target.";
        }

        if (food.protein() >= remainingProtein * 0.75) {
            return "Provides most of your remaining protein.";
        }

        if (food.protein() > 0) {
            return "Adds protein while staying within your calorie budget.";
        }

        return "Fits your remaining calorie budget.";
    }
}
