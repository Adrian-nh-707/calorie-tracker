
package com.group19.calorietrack.controller;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.repositories.TrainerGoalRecommendationRepository;
import com.group19.calorietrack.services.TrainerGoalRecommendationService;
import com.group19.calorietrack.services.TrainerGoalRecommendationService.RecommendationView;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainerGoalDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private TrainerGoalRecommendationRepository recommendationRepository;

    @Autowired
    private TrainerGoalRecommendationService recommendationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser trainer;
    private AppUser userA;
    private AppUser userB;

    @BeforeEach
    void setUp() {
        clearTestData();

        trainer = createUser(
                "trainer@example.com",
                "Tara",
                "Trainer",
                UserRole.TRAINER,
                null
        );

        userA = createUser(
                "goal-a@example.com",
                "Alice",
                "User",
                UserRole.USER,
                trainer
        );

        userB = createUser(
                "goal-b@example.com",
                "Bob",
                "User",
                UserRole.USER,
                trainer
        );
    }

    @AfterEach
    void cleanUp() {
        clearTestData();
    }

    // Clear test data
    private void clearTestData() {
        recommendationRepository.deleteAll();
        mealRepository.deleteAll();

        for (AppUser account : userRepository.findByRole(UserRole.USER)) {
            if (account.getAssignedTrainer() != null) {
                account.setAssignedTrainer(null);
                userRepository.save(account);
            }
        }

        userRepository.flush();
        userRepository.deleteAll();
    }

    //Create a test user
    private AppUser createUser(
            String email,
            String firstName,
            String lastName,
            UserRole role,
            AppUser assignedTrainer) {

        AppUser account = new AppUser();

        account.setEmail(email);
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setRole(role);
        account.setPassword(
                passwordEncoder.encode("TestPassword123")
        );
        account.setAssignedTrainer(assignedTrainer);

        return userRepository.save(account);
    }

    // Open user dashboard
    private MvcResult openDashboard(String email) throws Exception {
        return mockMvc.perform(
                get("/dashboard")
                    .with(user(email).roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andReturn();
    }

    @SuppressWarnings("unchecked")
    private List<RecommendationView> getRecommendations(
            MvcResult result) {

        assertNotNull(result.getModelAndView());

        Object recommendations = result
                .getModelAndView()
                .getModel()
                .get("trainerGoalRecommendations");

        assertNotNull(recommendations);

        return (List<RecommendationView>) recommendations;
    }

    @Test
    void testEmptyRecommendationsDisplayMessage() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("goal-a@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(
                containsString("No recommendations yet")
        ))
        .andReturn();

        assertTrue(getRecommendations(result).isEmpty());
    }

    @Test
    void testSavedRecommendationAppearsOnDashboard() throws Exception {
        recommendationService.createRecommendation(
                "trainer@example.com",
                userA.getId(),
                2200,
                140,
                "Increase your daily protein intake."
        );

        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("goal-a@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(
                containsString("Nutrition Goal Recommendation")
        ))
        .andExpect(content().string(
                containsString("Increase your daily protein intake.")
        ))
        .andReturn();

        List<RecommendationView> recommendations =
                getRecommendations(result);

        assertEquals(1, recommendations.size());
        assertEquals(2200, recommendations.get(0).calorieGoal());
        assertEquals(140, recommendations.get(0).proteinGoal());
        assertEquals("Tara Trainer", recommendations.get(0).trainerName());
    }

    @Test
    void testOtherUsersCannotSeePrivateRecommendations() throws Exception {
        recommendationService.createRecommendation(
                "trainer@example.com",
                userA.getId(),
                2300,
                160,
                "Private nutrition note for Alice."
        );

        MvcResult result = mockMvc.perform(
                get("/dashboard")
                    .with(user("goal-b@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(
                not(containsString("Private nutrition note for Alice."))
        ))
        .andReturn();

        assertTrue(getRecommendations(result).isEmpty());
    }

    @Test
    void testRecommendationsPersistAcrossRequests() throws Exception {
        recommendationService.createRecommendation(
                "trainer@example.com",
                userA.getId(),
                2000,
                120,
                "Keep tracking your meals."
        );

        MvcResult firstRequest = openDashboard(
                "goal-a@example.com"
        );

        MvcResult secondRequest = openDashboard(
                "goal-a@example.com"
        );

        assertEquals(
                1,
                getRecommendations(firstRequest).size()
        );

        assertEquals(
                1,
                getRecommendations(secondRequest).size()
        );

        assertEquals(
                "Keep tracking your meals.",
                getRecommendations(secondRequest).get(0).note()
        );
    }

    @Test
    void testRecommendationDoesNotAutomaticallyChangeUserGoal()
            throws Exception {

        recommendationService.createRecommendation(
                "trainer@example.com",
                userA.getId(),
                1800,
                130,
                "Suggested nutrition target."
        );

        MvcResult result = openDashboard(
                "goal-a@example.com"
        );

        assertEquals(
                2500,
                result.getModelAndView()
                        .getModel()
                        .get("calorieGoal")
        );

        assertEquals(
                1800,
                getRecommendations(result).get(0).calorieGoal()
        );
    }

    @Test
    void testUnassignedTrainerCannotCreateRecommendation() {
        AppUser otherTrainer = createUser(
                "other-trainer@example.com",
                "Other",
                "Trainer",
                UserRole.TRAINER,
                null
        );

        assertThrows(
                AccessDeniedException.class,
                () -> recommendationService.createRecommendation(
                        otherTrainer.getEmail(),
                        userA.getId(),
                        2000,
                        120,
                        "Unauthorized recommendation."
                )
        );
    }

    @Test
    void testDashboardRequiresCorrectRole() throws Exception {
        mockMvc.perform(
                get("/dashboard")
        )
        .andExpect(status().is3xxRedirection());

        mockMvc.perform(
                get("/dashboard")
                    .with(user("trainer@example.com").roles("TRAINER"))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void testTrainerNoteIsEscapedInHTML() throws Exception {
        recommendationService.createRecommendation(
                "trainer@example.com",
                userA.getId(),
                2200,
                140,
                "<script>alert('test')</script>"
        );

        mockMvc.perform(
                get("/dashboard")
                    .with(user("goal-a@example.com").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(
                not(containsString("<script>alert('test')</script>"))
        ))
        .andExpect(content().string(
                containsString("&lt;script&gt;")
        ));
    }
}
