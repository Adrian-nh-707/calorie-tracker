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
        Set<LocalDate> datesWithMeals = new HashSet<>();

        // Add calories from meals on the same date
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

        List<DayCalories> days = new ArrayList<>();

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

            days.add(new DayCalories(
                    date,
                    date.format(DAY_FORMAT),
                    date.format(DATE_FORMAT),
                    calories,
                    barHeight,
                    calories > calorieGoal
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

    // Information for one day
    public record DayCalories(
            LocalDate date,
            String dayLabel,
            String dateLabel,
            double calories,
            int barHeightPercent,
            boolean aboveGoal
    ) {
    }

    // Information for the entire week
    public record WeeklySummary(
            List<DayCalories> days,
            double totalCalories,
            double averageCalories,
            int daysLogged,
            int goalLinePercent,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}