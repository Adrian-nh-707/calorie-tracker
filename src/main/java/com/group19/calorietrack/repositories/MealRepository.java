package com.group19.calorietrack.repositories;

import com.group19.calorietrack.models.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

// Handles database operations for meals
public interface MealRepository extends JpaRepository<Meal, Long> {

}