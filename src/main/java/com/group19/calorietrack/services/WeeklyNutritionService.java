package com.group19.calorietrack.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;

@Service
public class WeeklyNutritionService {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final MealRepository mealRepository;

    public WeeklyNutritionService(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    // Get seven days of calories including today
    public WeeklySummary getWeeklySummary(
            Long userId,
            LocalDate today,
            int calorieGoal) {

        LocalDate startDate = today.minusDays(6);

        // Get only the logged-in user's meals
        List<Meal> meals = mealRepository.findByUser_IdAndMealDateBetween(
                userId,
                startDate,
                today
        );

        Map<LocalDate, Double> caloriesByDate = new HashMap<>();
        Map<LocalDate, List<Meal>> mealsByDate = new HashMap<>();
        Set<LocalDate> datesWithMeals = new HashSet<>();

        // Group meals by date and calculate totals
        for (Meal meal : meals) {
            LocalDate date = meal.getMealDate();

            if (date != null
                    && !date.isBefore(startDate)
                    && !date.isAfter(today)) {

                caloriesByDate.merge(
                        date,
                        meal.getCalories(),
                        Double::sum
                );

                mealsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(meal);
                datesWithMeals.add(date);
            }
        }

        // Find the largest number for chart scaling
        double highestCalories = caloriesByDate.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);

        double chartMaximum = Math.max(
                1,
                Math.max(highestCalories, calorieGoal) * 1.12
        );

        List<DayWithMeals> days = new ArrayList<>();

        double weeklyTotal = 0;

        // Create a value for every day, even empty days
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);

            double calories = caloriesByDate.getOrDefault(
                    date,
                    0.0
            );

            weeklyTotal += calories;

            int barHeight = (int) Math.round(
                    Math.max(0, calories) * 100 / chartMaximum
            );

            barHeight = Math.min(100, barHeight);

            List<Meal> dayMeals = mealsByDate.getOrDefault(date, List.of());

            days.add(new DayWithMeals(
                    date,
                    date.format(DAY_FORMAT),
                    date.format(DATE_FORMAT),
                    calories,
                    barHeight,
                    calories > calorieGoal,
                    dayMeals
            ));
        }

        int goalLineHeight = (int) Math.round(
                calorieGoal * 100 / chartMaximum
        );

        return new WeeklySummary(
                List.copyOf(days),
                weeklyTotal,
                weeklyTotal / 7,
                datesWithMeals.size(),
                goalLineHeight,
                startDate,
                today
        );
    }

    // Get details for a single day
    public DayDetails getDayDetails(
            Long userId,
            LocalDate date,
            int calorieGoal) {

        List<Meal> meals = mealRepository.findByUser_IdAndMealDate(
                userId,
                date
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

        boolean aboveGoal = totalCalories > calorieGoal;

        return new DayDetails(
                date,
                date.format(DAY_FORMAT),
                date.format(DATE_FORMAT),
                totalCalories,
                totalProtein,
                totalCarbs,
                totalFat,
                aboveGoal,
                meals
        );
    }

    // Information for one day with meals
    public record DayWithMeals(
            LocalDate date,
            String dayLabel,
            String dateLabel,
            double calories,
            int barHeightPercent,
            boolean aboveGoal,
            List<Meal> meals,
            double totalProtein,
            double totalCarbs,
            double totalFat
    ) {
        // Convenience constructor for backward compatibility
        public DayWithMeals(LocalDate date, String dayLabel, String dateLabel,
                           double calories, int barHeightPercent, boolean aboveGoal,
                           List<Meal> meals) {
            this(date, dayLabel, dateLabel, calories, barHeightPercent, aboveGoal,
                    meals, calculateSum(meals, "protein"),
                    calculateSum(meals, "carbs"), calculateSum(meals, "fat"));
        }

        private static double calculateSum(List<Meal> meals, String field) {
            double sum = 0;
            for (Meal m : meals) {
                switch (field) {
                    case "protein" -> sum += m.getProtein();
                    case "carbs" -> sum += m.getCarbs();
                    case "fat" -> sum += m.getFat();
                    default -> sum += 0;
                }
            }
            return sum;
        }
    }

    // Information for a single day
    public record DayDetails(
            LocalDate date,
            String dayLabel,
            String dateLabel,
            double calories,
            double protein,
            double carbs,
            double fat,
            boolean aboveGoal,
            List<Meal> meals
    ) {
    }

    // Information for the entire week
    public record WeeklySummary(
            List<DayWithMeals> days,
            double totalCalories,
            double averageCalories,
            int daysLogged,
            int goalLinePercent,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}