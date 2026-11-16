
package com.group19.calorietrack.controller;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import com.group19.calorietrack.models.Meal;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.services.WeeklyNutritionService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeeklyNutritionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser userA;
    private AppUser userB;

    @BeforeEach
    void setUp() {
        mealRepository.deleteAll();
        userRepository.deleteAll();

        userA = createUser("weekly-a@example.com");
        userB = createUser("weekly-b@example.com");
    }

    @AfterEach
    void cleanUp() {
        mealRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Create a regular test user
    private AppUser createUser(String email) {
        AppUser account = new AppUser();

        account.setFirstName("Test");
        account.setLastName("User");
        account.setEmail(email);
        account.setPassword(
                passwordEncoder.encode("TestPassword123")
        );
        account.setRole(UserRole.USER);

        return userRepository.save(account);
    }

    // Save a meal for one user
    private void addMeal(
            AppUser user,
            LocalDate date,
            double calories) {

        Meal meal = new Meal(
                "Test Meal",
                "Snack",
                date,
                calories,
                20,
                30,
                10
        );

        meal.setUser(user);
        mealRepository.save(meal);
    }

    // Open the dashboard as the selected user
    private MvcResult openDashboard(String email) throws Exception {
        return mockMvc.perform(
                get("/dashboard")
                    .with(user(email).roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andReturn();
    }

    // Read the weekly data from the dashboard response
    private WeeklyNutritionService.WeeklySummary getSummary(
            MvcResult result) {

        var modelAndView = result.getModelAndView();

        assertNotNull(modelAndView);

        Object summary = modelAndView
                .getModel()
                .get("weeklySummary");

        assertNotNull(summary);

        return (WeeklyNutritionService.WeeklySummary) summary;
    }

    @Test
    void testWeeklyChartRendersCorrectly() throws Exception {
        LocalDate today = LocalDate.now();

        addMeal(userA, today.minusDays(3), 1200);
        addMeal(userA, today, 300);

        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("weekly-a@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(content().string(
                containsString("Weekly Nutrition Progress")
        ))
        .andExpect(content().string(
                containsString("weekly-bar-grid")
        ))
        .andExpect(content().string(
                containsString("weekly-goal-line")
        ))
        .andReturn();

        var summary = getSummary(result);

        assertEquals(7, summary.days().size());
        assertEquals(1500.0, summary.totalCalories(), 0.001);
        assertEquals(1500.0 / 7, summary.averageCalories(), 0.001);
        assertEquals(2, summary.daysLogged());
    }

    @Test
    void testWeeklyChartOnlyShowsCurrentUsersMeals() throws Exception {
        LocalDate today = LocalDate.now();

        addMeal(userA, today, 500);
        addMeal(userB, today, 3100);

        MvcResult resultA = openDashboard(
                "weekly-a@example.com"
        );

        var summaryA = getSummary(resultA);

        assertEquals(500.0, summaryA.totalCalories(), 0.001);
        assertEquals(1, summaryA.daysLogged());

        MvcResult resultB = openDashboard(
                "weekly-b@example.com"
        );

        var summaryB = getSummary(resultB);

        assertEquals(3100.0, summaryB.totalCalories(), 0.001);
        assertEquals(1, summaryB.daysLogged());

        assertTrue(summaryB.days().get(6).aboveGoal());
        assertFalse(summaryA.days().get(6).aboveGoal());
    }

    @Test
    void testWeeklyChartShowsEmptyState() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("weekly-a@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(
                containsString(
                    "You haven't logged any meals in the last seven days."
                )
        ))
        .andReturn();

        var summary = getSummary(result);

        assertEquals(7, summary.days().size());
        assertEquals(0.0, summary.totalCalories(), 0.001);
        assertEquals(0, summary.daysLogged());

        assertTrue(summary.days().stream().allMatch(
                day -> day.calories() == 0
        ));
    }

    @Test
    void testWeeklyChartUpdatesWhenGoalChanges() throws Exception {
        LocalDate today = LocalDate.now();

        addMeal(userA, today, 2200);

        // Check the default calorie goal
        var defaultSummary = getSummary(
                openDashboard("weekly-a@example.com")
        );

        assertFalse(
                defaultSummary.days().get(6).aboveGoal()
        );

        // Update the calorie goal through the application
        MvcResult goalUpdate = mockMvc.perform(
                post("/dashboard/goal")
                    .with(user("weekly-a@example.com").roles("USER"))
                    .with(csrf())
                    .param("calorieGoal", "2000")
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"))
        .andReturn();

        // Get the session cookie
        Cookie sessionCookie = goalUpdate.getResponse()
                .getCookie("SESSION");

        assertNotNull(sessionCookie);

        // Open the dashboard using the updated session
        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("weekly-a@example.com").roles("USER"))
                    .cookie(sessionCookie)
        )
        .andExpect(status().isOk())
        .andReturn();

        var updatedSummary = getSummary(result);

        // The new goal should be 2000 calories
        assertEquals(
                2000,
                result.getModelAndView()
                        .getModel()
                        .get("calorieGoal")
        );

        // 2200 calories should exceed the new goal
        assertTrue(
                updatedSummary.days().get(6).aboveGoal()
        );

        // The daily goal line should move down
        assertTrue(
                updatedSummary.goalLinePercent()
                        < defaultSummary.goalLinePercent()
        );

        // The logged calories should not change
        assertEquals(
                2200.0,
                updatedSummary.totalCalories(),
                0.001
        );
    }

    @Test
    void testWeeklyChartExcludesDatesOutsideWeek() throws Exception {
        LocalDate today = LocalDate.now();

        addMeal(userA, today.minusDays(7), 5000);
        addMeal(userA, today, 200);
        addMeal(userA, today.plusDays(1), 3000);

        var summary = getSummary(
                openDashboard("weekly-a@example.com")
        );

        assertEquals(7, summary.days().size());
        assertEquals(200.0, summary.totalCalories(), 0.001);
        assertEquals(1, summary.daysLogged());
    }

    @Test
    void testWeeklyChartRequiresLogin() throws Exception {
        mockMvc.perform(
                get("/dashboard")
        )
        .andExpect(status().is3xxRedirection());
    }
}
