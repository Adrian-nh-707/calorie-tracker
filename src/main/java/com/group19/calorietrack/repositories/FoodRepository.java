package com.group19.calorietrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group19.calorietrack.models.Food;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Integer>{
    List<Food> findById(int id);
    List<Food> findByTitle(String title);
}

