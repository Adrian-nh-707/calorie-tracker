package com.group19.calorietrack.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;

class MealControllerTest {

    private MealRepository mealRepository;
    private UserRepository userRepository;
    private MealController controller;
    private RedirectAttributes redirectAttributes;
    private Principal principal;
    private AppUser user;

    private final LocalDate date = LocalDate.of(2026, 10, 27);
    private final String dateText = "2026-10-27";

    @BeforeEach
    void setUp() {
        mealRepository = mock(MealRepository.class);
        userRepository = mock(UserRepository.class);
        redirectAttributes = mock(RedirectAttributes.class);

        user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);

        principal = () -> "test@example.com";

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        controller = new MealController(
                mealRepository, userRepository);
    }

    private Meal sampleMeal() {
        Meal meal = new Meal(
                "Egg Sandwich", "Breakfast", date,
                320, 18, 40, 12);
        meal.setUser(user);
        return meal;
    }

    private String addMeal(String name, String type,
                           double calories, double protein,
                           double carbs, double fat) {
        return controller.addMeal(
                name, type, dateText,
                calories, protein, carbs, fat,
                principal, redirectAttributes);
    }

    private String updateMeal(Long id, String type,
                              double calories) {
        return controller.updateMeal(
                id, "Egg Sandwich", type, dateText,
                calories, 18, 40, 12,
                principal, redirectAttributes);
    }

    @Test
    void testAddMeal() {
        String result = addMeal(
                "Egg Sandwich", "Breakfast",
                320, 18, 40, 12);

        ArgumentCaptor<Meal> saved =
                ArgumentCaptor.forClass(Meal.class);

        verify(mealRepository).save(saved.capture());

        assertEquals(user, saved.getValue().getUser());
        assertEquals("Egg Sandwich", saved.getValue().getName());
        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testDuplicateBreakfast() {
        when(mealRepository
                .existsByUser_IdAndMealDateAndMealType(
                        1L, date, "Breakfast"))
                .thenReturn(true);

        String result = addMeal(
                "Toast", "Breakfast",
                200, 5, 30, 6);

        verify(mealRepository, never()).save(any(Meal.class));
        verify(redirectAttributes).addFlashAttribute(
                eq("error"), anyString());

        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testMultipleSnacks() {
        addMeal("Banana", "Snack", 100, 1, 27, 0);
        addMeal("Apple", "Snack", 95, 0, 25, 0);

        verify(mealRepository, times(2))
                .save(any(Meal.class));
    }

    @Test
    void testInvalidMeal() {
        String result = addMeal(
                "Chicken Rice", "Lunch",
                -100, 30, 50, 10);

        verify(mealRepository, never()).save(any(Meal.class));
        verify(redirectAttributes).addFlashAttribute(
                eq("error"), anyString());

        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testEmptyMealName() {
        addMeal("   ", "Breakfast", 320, 18, 40, 12);

        verify(mealRepository, never()).save(any(Meal.class));
        verify(redirectAttributes).addFlashAttribute(
                eq("error"), anyString());
    }

    @Test
    void testInvalidNutrition() {
        addMeal("Chicken Rice", "Lunch", 550, -10, 65, 15);

        verify(mealRepository, never()).save(any(Meal.class));
    }

    @Test
    void testDailyNutrition() {
        Meal breakfast = sampleMeal();
        Meal lunch = new Meal(
                "Chicken Rice", "Lunch", date,
                550, 35, 65, 15);
        lunch.setUser(user);

        when(mealRepository.findByUser_IdAndMealDate(1L, date))
                .thenReturn(List.of(breakfast, lunch));

        Model model = mock(Model.class);

        String result = controller.showMeals(
                dateText, model, principal);

        verify(model).addAttribute("totalCalories", 870.0);
        verify(model).addAttribute("totalProtein", 53.0);
        verify(model).addAttribute("totalCarbs", 105.0);
        verify(model).addAttribute("totalFat", 27.0);

        assertEquals("meals", result);
    }

    @Test
    void testEmptyMealHistory() {
        when(mealRepository.findByUser_IdAndMealDate(1L, date))
                .thenReturn(List.of());

        Model model = mock(Model.class);

        String result = controller.showMeals(
                dateText, model, principal);

        verify(model).addAttribute("totalCalories", 0.0);
        verify(model).addAttribute("totalProtein", 0.0);
        verify(model).addAttribute("meals", List.of());

        assertEquals("meals", result);
    }

    @Test
    void testOpenEditPage() {
        Meal meal = sampleMeal();

        when(mealRepository.findByIdAndUser_Id(1L, 1L))
                .thenReturn(Optional.of(meal));

        Model model = mock(Model.class);

        String result = controller.showEditMeal(
                1L, model, principal);

        verify(model).addAttribute("meal", meal);
        assertEquals("edit-meal", result);
    }

    @Test
    void testEditMeal() {
        Meal meal = sampleMeal();

        when(mealRepository.findByIdAndUser_Id(1L, 1L))
                .thenReturn(Optional.of(meal));

        String result = updateMeal(1L, "Breakfast", 400);

        assertEquals(400, meal.getCalories());
        verify(mealRepository).save(meal);

        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testEditDuplicateMeal() {
        Meal meal = sampleMeal();

        when(mealRepository.findByIdAndUser_Id(1L, 1L))
                .thenReturn(Optional.of(meal));

        when(mealRepository
                .existsByUser_IdAndMealDateAndMealTypeAndIdNot(
                        1L, date, "Lunch", 1L))
                .thenReturn(true);

        String result = updateMeal(1L, "Lunch", 400);

        verify(mealRepository, never()).save(any(Meal.class));
        verify(redirectAttributes).addFlashAttribute(
                eq("error"), anyString());

        assertEquals("redirect:/meals/edit/1", result);
    }

    @Test
    void testEditSameMealType() {
        Meal meal = sampleMeal();

        when(mealRepository.findByIdAndUser_Id(1L, 1L))
                .thenReturn(Optional.of(meal));

        String result = updateMeal(1L, "Breakfast", 400);

        verify(mealRepository)
                .existsByUser_IdAndMealDateAndMealTypeAndIdNot(
                        1L, date, "Breakfast", 1L);

        verify(mealRepository).save(meal);
        assertEquals(400, meal.getCalories());
        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testDeleteMeal() {
        Meal meal = sampleMeal();

        when(mealRepository.findByIdAndUser_Id(1L, 1L))
                .thenReturn(Optional.of(meal));

        String result = controller.deleteMeal(
                1L, principal, redirectAttributes);

        verify(mealRepository).delete(meal);
        assertEquals("redirect:/meals?date=" + dateText, result);
    }

    @Test
    void testDeleteMissingMeal() {
        when(mealRepository.findByIdAndUser_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.deleteMeal(
                        99L, principal, redirectAttributes));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(mealRepository, never()).delete(any(Meal.class));
    }

    @Test
    void testEditMissingMeal() {
        when(mealRepository.findByIdAndUser_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> updateMeal(99L, "Lunch", 400));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(mealRepository, never()).save(any(Meal.class));
    }

    @Test
    void testCannotViewOtherUsersMeal() {
        when(mealRepository.findByUser_IdAndMealDate(1L, date))
                .thenReturn(List.of());

        Model model = mock(Model.class);
        controller.showMeals(dateText, model, principal);

        verify(mealRepository)
                .findByUser_IdAndMealDate(1L, date);

        verify(model).addAttribute("meals", List.of());
    }

    @Test
    void testCannotEditOtherUsersMeal() {
        // Meal 55 belongs to another user
        when(mealRepository.findByIdAndUser_Id(55L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> updateMeal(55L, "Lunch", 400));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(mealRepository, never()).save(any(Meal.class));
    }

    @Test
    void testCannotDeleteOtherUsersMeal() {
        // Meal 55 belongs to another user
        when(mealRepository.findByIdAndUser_Id(55L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.deleteMeal(
                        55L, principal, redirectAttributes));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(mealRepository, never()).delete(any(Meal.class));
    }

    @Test
    void testUnauthenticatedUserCannotAddMeal() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.addMeal(
                        "Egg Sandwich", "Breakfast", dateText,
                        320, 18, 40, 12,
                        null, redirectAttributes));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(mealRepository, never()).save(any(Meal.class));
    }
}
