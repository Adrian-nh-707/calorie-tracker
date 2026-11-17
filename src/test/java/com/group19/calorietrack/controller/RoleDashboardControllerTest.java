package com.group19.calorietrack.controller;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.group19.calorietrack.messaging.MessageService;
import com.group19.calorietrack.models.TrainerGoalRecommendation;
import com.group19.calorietrack.repositories.MealRepository;
import com.group19.calorietrack.repositories.TrainerGoalRecommendationRepository;
import com.group19.calorietrack.services.TrainerGoalRecommendationService;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

class RoleDashboardControllerTest {

    private static final String TRAINER_EMAIL = "trainer@example.com";
    private static final Long USER_ID = 20L;

    private RoleDashboardController controller;

    private UserRepository userRepository;
    private MealRepository mealRepository;
    private TrainerGoalRecommendationRepository recommendationRepository;
    private AppUser trainer;
    private AppUser user;

    private Authentication authentication;

    private MessageService messageService;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        mealRepository = mock(MealRepository.class);
        recommendationRepository =
            mock(TrainerGoalRecommendationRepository.class);
        trainer = mock(AppUser.class);
        user = mock(AppUser.class);

        authentication = mock(Authentication.class);

        messageService = mock(MessageService.class);

        when(authentication.getName()).thenReturn(TRAINER_EMAIL);

        when(trainer.getId()).thenReturn(10L);
        when(trainer.getRole()).thenReturn(UserRole.TRAINER);

        when(user.getId()).thenReturn(USER_ID);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getAssignedTrainer()).thenReturn(trainer);

        when(userRepository.findByEmail(TRAINER_EMAIL))
                .thenReturn(Optional.of(trainer));

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(recommendationRepository.save(
                any(TrainerGoalRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainerGoalRecommendationService recommendationService =
                new TrainerGoalRecommendationService(
                        recommendationRepository,
                        userRepository
                );

        controller = new RoleDashboardController(
            userRepository,
            mealRepository,
            messageService,
            recommendationService
        );
    }

    @Test
    void testTrainerCanSubmitRecommendationForAssignedUser() {

        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        String result = controller.createGoalRecommendation(
                authentication,
                USER_ID,
                2200,
                140,
                "Increase protein intake.",
                redirectAttributes
        );

        assertEquals("redirect:/trainer/dashboard", result);

        ArgumentCaptor<TrainerGoalRecommendation> captor =
                ArgumentCaptor.forClass(TrainerGoalRecommendation.class);

        verify(recommendationRepository).save(captor.capture());

        TrainerGoalRecommendation savedRecommendation =
                captor.getValue();

        assertSame(trainer, savedRecommendation.getTrainer());
        assertSame(user, savedRecommendation.getUser());

        assertEquals(2200, savedRecommendation.getCalorieGoal());
        assertEquals(140, savedRecommendation.getProteinGoal());

        assertEquals(
                "Increase protein intake.",
                savedRecommendation.getNote()
        );

        assertEquals(
                "Goal recommendation sent successfully.",
                redirectAttributes.getFlashAttributes().get("successMessage")
        );
    }

    @Test
    void testTrainerCannotSubmitRecommendationForUnassignedUser() {

        AppUser otherTrainer = mock(AppUser.class);
        when(otherTrainer.getId()).thenReturn(99L);

        when(user.getAssignedTrainer()).thenReturn(otherTrainer);

        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        String result = controller.createGoalRecommendation(
                authentication,
                USER_ID,
                2200,
                140,
                "Test recommendation.",
                redirectAttributes
        );

        assertEquals("redirect:/trainer/dashboard", result);

        assertEquals(
                "You can only recommend goals to your assigned users.",
                redirectAttributes.getFlashAttributes().get("errorMessage")
        );

        verify(recommendationRepository, never())
                .save(any(TrainerGoalRecommendation.class));
    }
}