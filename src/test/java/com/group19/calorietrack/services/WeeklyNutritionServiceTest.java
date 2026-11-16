package com.group19.calorietrack.services;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;

class WeeklyNutritionServiceTest {

    private MealRepository mealRepository;
    private WeeklyNutritionService service;

    private final LocalDate today = LocalDate.of(2026, 11, 13);
    private final LocalDate startDate = today.minusDays(6);

    @BeforeEach
    void setUp() {
        mealRepository = mock(MealRepository.class);
        service = new WeeklyNutritionService(mealRepository);
    }

    private Meal meal(LocalDate date, double calories) {
        return new Meal(
                "Test Meal",
                "Snack",
                date,
                calories,
                0,
                0,
                0
        );
    }

    @Test
    void testSevenDaysAndDailyTotals() {

        when(mealRepository.findByUser_IdAndMealDateBetween(
                1L, startDate, today
        )).thenReturn(List.of(
                meal(startDate, 500),
                meal(startDate, 300),
                meal(today.minusDays(2), 1500),
                meal(today, 2600)
        ));

        var summary = service.getWeeklySummary(
                1L, today, 2500
        );

        assertEquals(7, summary.days().size());

        assertEquals(startDate, summary.days().get(0).date());
        assertEquals(today, summary.days().get(6).date());

        assertEquals(
                800.0,
                summary.days().get(0).calories(),
                0.001
        );

        assertEquals(
                0.0,
                summary.days().get(1).calories(),
                0.001
        );

        assertEquals(
                2600.0,
                summary.days().get(6).calories(),
                0.001
        );

        assertEquals(4900.0, summary.totalCalories(), 0.001);
        assertEquals(700.0, summary.averageCalories(), 0.001);
        assertEquals(3, summary.daysLogged());

        assertTrue(summary.days().get(6).aboveGoal());
        assertFalse(summary.days().get(0).aboveGoal());
    }

    @Test
    void testNoMeals() {

        var summary = service.getWeeklySummary(
                1L, today, 2500
        );

        assertEquals(7, summary.days().size());
        assertEquals(0.0, summary.totalCalories(), 0.001);
        assertEquals(0.0, summary.averageCalories(), 0.001);
        assertEquals(0, summary.daysLogged());

        assertTrue(summary.days().stream().allMatch(
                day -> day.calories() == 0
                        && day.barHeightPercent() == 0
        ));
    }

    @Test
    void testOnlyCurrentUserMeals() {

        service.getWeeklySummary(
                42L, today, 2500
        );

        verify(mealRepository).findByUser_IdAndMealDateBetween(
                42L,
                startDate,
                today
        );
    }

    @Test
    void testCaloriesAboveGoal() {

        when(mealRepository.findByUser_IdAndMealDateBetween(
                1L, startDate, today
        )).thenReturn(List.of(
                meal(today, 4000)
        ));

        var summary = service.getWeeklySummary(
                1L, today, 2500
        );

        assertTrue(summary.days().get(6).aboveGoal());

        assertTrue(
                summary.days().get(6).barHeightPercent()
                        > summary.goalLinePercent()
        );

        assertTrue(
                summary.days().get(6).barHeightPercent() <= 100
        );
    }

    @Test
    void testDifferentGoals() {

        var first = service.getWeeklySummary(
                1L, today, 2000
        );

        var second = service.getWeeklySummary(
                1L, today, 3000
        );

        assertEquals(7, first.days().size());
        assertEquals(7, second.days().size());

        assertTrue(first.goalLinePercent() > 0);
        assertTrue(second.goalLinePercent() > 0);
    }

    @Test
    void testDatesOutsideWeekAreIgnored() {

        when(mealRepository.findByUser_IdAndMealDateBetween(
                1L, startDate, today
        )).thenReturn(List.of(
                meal(today.minusDays(7), 1000),
                meal(today.plusDays(1), 2000),
                meal(today, 500)
        ));

        var summary = service.getWeeklySummary(
                1L, today, 2500
        );

        assertEquals(500.0, summary.totalCalories(), 0.001);
        assertEquals(1, summary.daysLogged());
    }
}