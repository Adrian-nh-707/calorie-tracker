
package com.group19.calorietrack.controller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class TrainerGoalRecommendationsTemplateTest {

    //Load a template or stylesheet
    private String loadResource(String path) throws IOException {

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {

            assertNotNull(input, "Missing resource: " + path);

            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    @Test
    void testRecommendationFragmentExists() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains(
                "th:fragment=\"userGoals\""
        ));
    }

    @Test
    void testRecommendationDisplaysNutritionTargets() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains("recommendation.calorieGoal"));
        assertTrue(html.contains("recommendation.proteinGoal"));
        assertTrue(html.contains("recommendation.trainerName"));
    }

    @Test
    void testRecommendationDisplaysTrainerNote() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains("recommendation.note"));
        assertTrue(html.contains("Trainer's Note"));
    }

    @Test
    void testRecommendationDisplaysDate() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains("recommendation.createdAtLabel"));
    }

    @Test
    void testEmptyRecommendationsMessage() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains("No recommendations yet"));
        assertTrue(html.contains(
                "#lists.isEmpty(trainerGoalRecommendations)"
        ));
    }

    @Test
    void testMultipleRecommendationsSupported() throws IOException {

        String html = loadResource(
                "templates/fragments/trainer-goal-recommendations.html"
        );

        assertTrue(html.contains(
                "th:each=\"recommendation : ${trainerGoalRecommendations}\""
        ));
    }

    @Test
    void testMobileStylingExists() throws IOException {

        String css = loadResource(
                "static/css/trainer-goal-recommendations.css"
        );

        assertTrue(css.contains("@media (max-width: 600px)"));
        assertTrue(css.contains(".trainer-goal-values"));
        assertTrue(css.contains(".trainer-goal-note"));
    }
}
