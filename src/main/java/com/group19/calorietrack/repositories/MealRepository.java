
package com.group19.calorietrack.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group19.calorietrack.models.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {

    // Find meals from a specific date
    List<Meal> findByMealDate(LocalDate mealDate);

    // Check if this meal type already exists
    boolean existsByMealDateAndMealType(
        LocalDate mealDate,
        String mealType
    );

    // Same check, but ignore the meal we're editing
    boolean existsByMealDateAndMealTypeAndIdNot(
        LocalDate mealDate,
        String mealType,
        Long id
    );
}
