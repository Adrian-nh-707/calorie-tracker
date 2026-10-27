
package com.group19.calorietrack.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group19.calorietrack.models.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByMealDate(LocalDate mealDate);

    boolean existsByMealDateAndMealType(
        LocalDate mealDate,
        String mealType
    );
}
