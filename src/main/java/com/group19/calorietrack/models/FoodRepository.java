package com.group19.calorietrack.models;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Integer>{
    List<Food> findById(int id);
    List<Food> findByTitle(String title);
}

