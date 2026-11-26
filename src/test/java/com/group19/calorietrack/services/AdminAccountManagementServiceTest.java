package com.group19.calorietrack.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

class AdminAccountManagementServiceTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final Long USER_ID = 2L;

    private AdminAccountManagementService service;

    private UserRepository userRepository;
    private AppUser admin;
    private AppUser targetAccount;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        admin = createAccount(1L, UserRole.ADMIN, true);
        targetAccount = createAccount(USER_ID, UserRole.USER, true);

        when(userRepository.findByEmail(ADMIN_EMAIL))
                .thenReturn(Optional.of(admin));

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(targetAccount));

        service = new AdminAccountManagementService(userRepository);
    }

    // Deactivation tests
    @Test
    void deactivateUserSuccessfully() {

        service.deactivateAccount(USER_ID, ADMIN_EMAIL);

        verify(targetAccount).setEnabled(false);
        verify(userRepository).save(targetAccount);
    }

    @Test
    void deactivateTrainerWithoutAssignedUsersSuccessfully() {

        when(targetAccount.getRole()).thenReturn(UserRole.TRAINER);

        when(userRepository.countByAssignedTrainer_IdAndRole(
                USER_ID, UserRole.USER))
                .thenReturn(0L);

        service.deactivateAccount(USER_ID, ADMIN_EMAIL);

        verify(targetAccount).setEnabled(false);
        verify(userRepository).save(targetAccount);
    }

    @Test
    void deactivateTrainerWithAssignedUsersIsRejected() {

        when(targetAccount.getRole()).thenReturn(UserRole.TRAINER);

        when(userRepository.countByAssignedTrainer_IdAndRole(
                USER_ID, UserRole.USER))
                .thenReturn(2L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        assertEquals(
                "Reassign or unassign this trainer's users before deactivating the account.",
                exception.getMessage()
        );

        verify(userRepository, never()).save(targetAccount);
    }

    @Test
    void deactivateAdminAccountIsRejected() {

        when(targetAccount.getRole()).thenReturn(UserRole.ADMIN);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(targetAccount);
    }

    @Test
    void administratorCannotDeactivateOwnAccount() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deactivateAccount(1L, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(admin);
    }

    @Test
    void nonAdminCannotDeactivateAccounts() {

        when(admin.getRole()).thenReturn(UserRole.USER);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(targetAccount);
    }

    @Test
    void alreadyInactiveAccountCannotBeDeactivatedAgain() {

        when(targetAccount.isEnabled()).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(targetAccount);
    }

    // Reactivation tests
    @Test
    void reactivateUserSuccessfully() {

        when(targetAccount.isEnabled()).thenReturn(false);

        service.reactivateAccount(USER_ID, ADMIN_EMAIL);

        verify(targetAccount).setEnabled(true);
        verify(userRepository).save(targetAccount);
    }

    @Test
    void reactivateTrainerSuccessfully() {

        when(targetAccount.getRole()).thenReturn(UserRole.TRAINER);
        when(targetAccount.isEnabled()).thenReturn(false);

        service.reactivateAccount(USER_ID, ADMIN_EMAIL);

        verify(targetAccount).setEnabled(true);
        verify(userRepository).save(targetAccount);
    }

    @Test
    void reactivateAdminAccountIsRejected() {

        when(targetAccount.getRole()).thenReturn(UserRole.ADMIN);
        when(targetAccount.isEnabled()).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(targetAccount);
    }

    @Test
    void alreadyActiveAccountCannotBeReactivatedAgain() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reactivateAccount(USER_ID, ADMIN_EMAIL)
        );

        verify(userRepository, never()).save(targetAccount);
    }

    private AppUser createAccount(
            Long id,
            UserRole role,
            boolean enabled) {

        AppUser account = mock(AppUser.class);

        when(account.getId()).thenReturn(id);
        when(account.getRole()).thenReturn(role);
        when(account.isEnabled()).thenReturn(enabled);

        return account;
    }
}