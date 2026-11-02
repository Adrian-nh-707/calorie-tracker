package com.group19.calorietrack.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group19.calorietrack.models.Food;
import com.group19.calorietrack.repositories.FoodRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;



@Controller
public class FoodController {
    @Autowired 
    private FoodRepository foodRepo;

    @GetMapping("/food/viewFood") // not implemented yet
    public String getFood(Model model){



        return "food/viewFood";
    }

    @PostMapping("/food/add")
    public String addFood(@RequestParam Map<String, String> newfood, HttpServletResponse response){
        System.out.println("Add Food");
        String title = newfood.get("title");
        String serving = newfood.get("serving");
        String calories = newfood.get("calories");
        String total_fat = newfood.get("total_fat");
        String saturated_fat = newfood.get("saturated_fat");
        String protein = newfood.get("protein");
        String sodium = newfood.get("sodium");
        String potassium = newfood.get("potassium");
        String dietary_fiber = newfood.get("dietary_fiber");
        String cholesterol = newfood.get("cholesterol");
        String sugars = newfood.get("sugars");
        String total_carbohydrate = newfood.get("total_carbohydrate");
        String recipe = newfood.get("recipe");
        String ingredients = newfood.get("ingredients");

        foodRepo.save(new Food(title, serving, calories, total_fat, saturated_fat, protein, sodium, potassium, dietary_fiber, cholesterol, sugars, total_carbohydrate, recipe, ingredients));

        response.setStatus(201); // just created new object;

        return "food/addedFood"; //just leads to success page will most likely change later
    }
    
}




