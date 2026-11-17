package com.group19.calorietrack.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group19.calorietrack.models.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {

    // Find only the current user's meals
    List<Meal> findByUser_IdAndMealDate(
        Long userId,
        LocalDate mealDate
    );

    // Find a meal only if it belongs to the user
    Optional<Meal> findByIdAndUser_Id(
        Long id,
        Long userId
    );

    // Check duplicate meal types for this user
    boolean existsByUser_IdAndMealDateAndMealType(
        Long userId,
        LocalDate mealDate,
        String mealType
    );

    // Ignore the meal currently being edited
    boolean existsByUser_IdAndMealDateAndMealTypeAndIdNot(
        Long userId,
        LocalDate mealDate,
        String mealType,
        Long id
    );

    // Find meals for a group of users on a specific date
    @Query("SELECT m FROM Meal m WHERE m.user.id IN :userIds AND m.mealDate = :mealDate")
    List<Meal> findByUserIdsAndMealDate(
        @Param("userIds") List<Long> userIds,
        @Param("mealDate") LocalDate mealDate
    );

    // Get meals for the weekly chart
    List<Meal> findByUser_IdAndMealDateBetween(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    );
}