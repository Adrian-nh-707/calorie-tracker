package com.group19.calorietrack.dto;

import com.group19.calorietrack.user.AppUser;

public class NutritionProgress {

    private AppUser user;
    private double calories;
    private double protein;
    private double carbs;
    private double fat;
    private int mealCount;

    public NutritionProgress(AppUser user) {
        this.user = user;
        this.calories = 0;
        this.protein = 0;
        this.carbs = 0;
        this.fat = 0;
        this.mealCount = 0;
    }

    public void addMeal(double calories, double protein,
                        double carbs, double fat) {
        this.calories += calories;
        this.protein += protein;
        this.carbs += carbs;
        this.fat += fat;
        this.mealCount++;
    }

    public AppUser getUser() {
        return user;
    }

    public double getCalories() {
        return calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getFat() {
        return fat;
    }

    public int getMealCount() {
        return mealCount;
    }
}