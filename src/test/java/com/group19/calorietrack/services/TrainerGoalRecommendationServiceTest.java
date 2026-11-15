
package com.group19.calorietrack.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;

import com.group19.calorietrack.models.TrainerGoalRecommendation;
import com.group19.calorietrack.repositories.TrainerGoalRecommendationRepository;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

class TrainerGoalRecommendationServiceTest {

    private TrainerGoalRecommendationRepository recommendationRepository;
    private UserRepository userRepository;
    private TrainerGoalRecommendationService service;

    private AppUser trainer;
    private AppUser regularUser;

    @BeforeEach
    void setUp() {

        recommendationRepository =
                mock(TrainerGoalRecommendationRepository.class);

        userRepository = mock(UserRepository.class);

        service = new TrainerGoalRecommendationService(
                recommendationRepository,
                userRepository
        );

        // Create fake trainer
        trainer = mock(AppUser.class);

        when(trainer.getId()).thenReturn(10L);
        when(trainer.getRole()).thenReturn(UserRole.TRAINER);
        when(trainer.getFirstName()).thenReturn("John");
        when(trainer.getLastName()).thenReturn("Smith");

        // Create fake regular user
        regularUser = mock(AppUser.class);

        when(regularUser.getId()).thenReturn(20L);
        when(regularUser.getRole()).thenReturn(UserRole.USER);
        when(regularUser.getAssignedTrainer()).thenReturn(trainer);

        when(userRepository.findByEmail("trainer@example.com"))
                .thenReturn(Optional.of(trainer));

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(regularUser));

        when(userRepository.findById(20L))
                .thenReturn(Optional.of(regularUser));

        // Return the saved recommendation
        when(recommendationRepository.save(
                any(TrainerGoalRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testTrainerCanCreateRecommendation() {

        var recommendation = service.createRecommendation(
                "trainer@example.com",
                20L,
                2200,
                140,
                "Increase protein intake."
        );

        assertNotNull(recommendation);

        assertSame(trainer, recommendation.getTrainer());
        assertSame(regularUser, recommendation.getUser());

        assertEquals(2200, recommendation.getCalorieGoal());
        assertEquals(140, recommendation.getProteinGoal());

        assertEquals(
                "Increase protein intake.",
                recommendation.getNote()
        );

        assertNotNull(recommendation.getCreatedAt());

        verify(recommendationRepository).save(
                any(TrainerGoalRecommendation.class)
        );
    }

    @Test
    void testRecommendationNoteIsTrimmed() {

        var recommendation = service.createRecommendation(
                "trainer@example.com",
                20L,
                2000,
                120,
                "  Eat more protein.  "
        );

        assertEquals(
                "Eat more protein.",
                recommendation.getNote()
        );
    }

    @Test
    void testRecommendationAllowsEmptyNote() {

        var recommendation = service.createRecommendation(
                "trainer@example.com",
                20L,
                2000,
                120,
                null
        );

        assertEquals("", recommendation.getNote());
    }

    @Test
    void testNonTrainerCannotCreateRecommendation() {

        when(trainer.getRole()).thenReturn(UserRole.USER);

        assertThrows(
                AccessDeniedException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testTrainerCannotRecommendToUnassignedUser() {

        AppUser otherTrainer = mock(AppUser.class);
        when(otherTrainer.getId()).thenReturn(99L);

        when(regularUser.getAssignedTrainer())
                .thenReturn(otherTrainer);

        assertThrows(
                AccessDeniedException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testUserWithoutTrainerCannotReceiveRecommendation() {

        when(regularUser.getAssignedTrainer()).thenReturn(null);

        assertThrows(
                AccessDeniedException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testTrainerCannotRecommendToAnotherTrainer() {
        when(regularUser.getRole()).thenReturn(UserRole.TRAINER);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testInvalidCalorieGoalsAreRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        0,
                        140,
                        "Test"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        10001,
                        140,
                        "Test"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        null,
                        140,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testInvalidProteinGoalsAreRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        -1,
                        "Test"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        501,
                        "Test"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        null,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testLongNoteIsRejected() {

        String longNote = "a".repeat(1001);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        longNote
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testUnknownUserIsRejected() {

        when(userRepository.findById(20L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createRecommendation(
                        "trainer@example.com",
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testUserCanViewOwnRecommendations() {

        TrainerGoalRecommendation recommendation =
                new TrainerGoalRecommendation(
                        trainer,
                        regularUser,
                        2200,
                        140,
                        "Increase protein intake."
                );

        recommendation.setCreatedAt(
                LocalDateTime.of(2026, 11, 14, 15, 30)
        );

        when(recommendationRepository.findByUser_IdOrderByCreatedAtDesc(
                20L
        )).thenReturn(List.of(recommendation));

        var results = service.getRecommendationsForUser(
                "user@example.com"
        );

        assertEquals(1, results.size());

        var result = results.get(0);

        assertEquals("John Smith", result.trainerName());
        assertEquals(2200, result.calorieGoal());
        assertEquals(140, result.proteinGoal());

        assertEquals(
                "Increase protein intake.",
                result.note()
        );

        assertEquals("Nov 14, 2026", result.createdAtLabel());

        verify(recommendationRepository)
                .findByUser_IdOrderByCreatedAtDesc(20L);
    }

    @Test
    void testUserWithNoRecommendationsGetsEmptyList() {

        when(recommendationRepository.findByUser_IdOrderByCreatedAtDesc(
                20L
        )).thenReturn(List.of());

        var results = service.getRecommendationsForUser(
                "user@example.com"
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void testOnlySelectedUsersRecommendationsAreQueried() {

        service.getRecommendationsForUser(
                "user@example.com"
        );

        verify(recommendationRepository)
                .findByUser_IdOrderByCreatedAtDesc(20L);
    }

    @Test
    void testTrainerCannotUseRegularUserRecommendationView() {

        assertThrows(
                AccessDeniedException.class,
                () -> service.getRecommendationsForUser(
                        "trainer@example.com"
                )
        );
    }

    @Test
    void testMissingAuthenticationIsRejected() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.createRecommendation(
                        null,
                        20L,
                        2200,
                        140,
                        "Test"
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> service.getRecommendationsForUser(null)
        );
    }
}
