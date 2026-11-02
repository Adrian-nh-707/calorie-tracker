package com.group19.calorietrack.models;

import java.security.DEREncodable;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity 
@Table(name="food")


public class Food {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String title;
    String serving;
    String calories;
    String total_fat;
    String saturated_fat;
    String protein;
    String sodium;
    String potassium;
    String dietary_fiber;
    String cholesterol;
    String sugars;
    String total_carbohydrate;

    @Column(columnDefinition = "TEXT")
    String recipe;

    @Column(columnDefinition = "TEXT")
    String ingredients;


    public Food(String title, String serving, String calories, String total_fat, String saturated_fat,
            String protein, String sodium, String potassium, String dietary_fiber, String cholesterol, String sugars,
            String total_carbohydrate, String recipe, String ingredients) {

        this.title = title;
        this.serving = serving;
        this.calories = calories;
        this.total_fat = total_fat;
        this.saturated_fat = saturated_fat;
        this.protein = protein;
        this.sodium = sodium;
        this.potassium = potassium;
        this.dietary_fiber = dietary_fiber;
        this.cholesterol = cholesterol;
        this.sugars = sugars;
        this.total_carbohydrate = total_carbohydrate;
        this.recipe = recipe;
        this.ingredients = ingredients;
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getServing() {
        return serving;
    }
    public void setServing(String serving) {
        this.serving = serving;
    }
    public String getCalories() {
        return calories;
    }
    public void setCalories(String calories) {
        this.calories = calories;
    }
    public String getTotal_fat() {
        return total_fat;
    }
    public void setTotal_fat(String total_fat) {
        this.total_fat = total_fat;
    }
    public String getSaturated_fat() {
        return saturated_fat;
    }
    public void setSaturated_fat(String saturated_fat) {
        this.saturated_fat = saturated_fat;
    }
    public String getProtein() {
        return protein;
    }
    public void setProtein(String protein) {
        this.protein = protein;
    }
    public String getSodium() {
        return sodium;
    }
    public void setSodium(String sodium) {
        this.sodium = sodium;
    }
    public String getPotassium() {
        return potassium;
    }
    public void setPotassium(String potassium) {
        this.potassium = potassium;
    }
    public String getDietary_fiber() {
        return dietary_fiber;
    }
    public void setDietary_fiber(String dietary_fiber) {
        this.dietary_fiber = dietary_fiber;
    }
    public String getCholesterol() {
        return cholesterol;
    }
    public void setCholesterol(String cholesterol) {
        this.cholesterol = cholesterol;
    }
    public String getSugars() {
        return sugars;
    }
    public void setSugars(String sugars) {
        this.sugars = sugars;
    }
    public String getTotal_carbohydrate() {
        return total_carbohydrate;
    }
    public void setTotal_carbohydrate(String total_carbohydrate) {
        this.total_carbohydrate = total_carbohydrate;
    }
    public String getRecipe() {
        return recipe;
    }
    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }
    public String getIngredients() {
        return ingredients;
    }
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }


    
            // form.appendChild(form_title); //title
            // form.appendChild(serving);
            // form.appendChild(calories);
            // form.appendChild(total_fat);
            // form.appendChild(saturated_fat);
            // form.appendChild(protein);
            // form.appendChild(sodium);
            // form.appendChild(potassium);
            // form.appendChild(dietary_fiber);
            // form.appendChild(cholesterol);
            // form.appendChild(sugars);
            // form.appendChild(total_carbohydrate);
            // form.appendChild(recipe);
            // form.appendChild(ingredients);
            // form.appendChild(submit);
}

