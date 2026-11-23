package com.group19.calorietrack.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@SpringBootTest
@ActiveProfiles("test")
class AccountSettingsServiceTest {

    private static final String TEST_PASSWORD = "StrongPass123";

    @Autowired
    private AccountSettingsService accountSettingsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        userRepository.deleteAll();
    }

    @Test
    void getProfileReturnsCorrectUserInformation() {

        saveUser("user@example.com", UserRole.USER);

        ProfileUpdateForm form =
                accountSettingsService.getProfile("user@example.com");

        assertEquals("Test", form.getFirstName());
        assertEquals("Account", form.getLastName());
        assertEquals("user@example.com", form.getEmail());
    }

    @Test
    void updateProfileChangesFirstAndLastName() {

        saveUser("user@example.com", UserRole.USER);

        ProfileUpdateForm form = createProfileForm(
                "Alex",
                "Morgan",
                "user@example.com"
        );

        boolean emailChanged =
                accountSettingsService.updateProfile(
                        "user@example.com", form
                );

        AppUser updated =
                userRepository.findByEmail("user@example.com").orElseThrow();

        assertFalse(emailChanged);
        assertEquals("Alex", updated.getFirstName());
        assertEquals("Morgan", updated.getLastName());
    }

    @Test
    void updateProfileChangesEmailSuccessfully() {

        saveUser("user@example.com", UserRole.USER);

        ProfileUpdateForm form = createProfileForm(
                "Alex",
                "Morgan",
                "New.User@Example.com"
        );

        form.setCurrentPassword(TEST_PASSWORD);

        boolean emailChanged =
                accountSettingsService.updateProfile(
                        "user@example.com", form
                );

        assertTrue(emailChanged);
        assertTrue(userRepository.findByEmail("user@example.com").isEmpty());

        AppUser updated =
                userRepository.findByEmail("new.user@example.com").orElseThrow();

        assertEquals("Alex", updated.getFirstName());
        assertEquals("Morgan", updated.getLastName());
    }

    @Test
    void updateProfileRejectsDuplicateEmail() {

        saveUser("user@example.com", UserRole.USER);
        saveUser("existing@example.com", UserRole.USER);

        ProfileUpdateForm form = createProfileForm(
                "Alex",
                "Morgan",
                "existing@example.com"
        );

        form.setCurrentPassword(TEST_PASSWORD);

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.updateProfile(
                        "user@example.com", form
                )
        );

        assertTrue(userRepository.findByEmail("user@example.com").isPresent());
    }

    @Test
    void updateProfileRejectsIncorrectCurrentPasswordForEmailChange() {

        saveUser("user@example.com", UserRole.USER);

        ProfileUpdateForm form = createProfileForm(
                "Alex",
                "Morgan",
                "new@example.com"
        );

        form.setCurrentPassword("WrongPassword123");

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.updateProfile(
                        "user@example.com", form
                )
        );

        assertTrue(userRepository.findByEmail("user@example.com").isPresent());
    }

    @Test
    void updateProfileRejectsEmailChangeWithoutCurrentPassword() {

        saveUser("user@example.com", UserRole.USER);

        ProfileUpdateForm form = createProfileForm(
                "Alex",
                "Morgan",
                "new@example.com"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.updateProfile(
                        "user@example.com", form
                )
        );
    }

    @Test
    void getAccountInformationReturnsAssignedTrainer() {

        AppUser trainer = saveUser(
                "trainer@example.com", UserRole.TRAINER
        );

        AppUser user = saveUser(
                "user@example.com", UserRole.USER
        );

        user.setAssignedTrainer(trainer);
        userRepository.save(user);

        AccountInformation information =
                accountSettingsService.getAccountInformation(
                        "user@example.com"
                );

        assertEquals("USER", information.getRole());
        assertEquals("Test Account", information.getAssignedTrainerName());
        assertEquals("trainer@example.com",
                information.getAssignedTrainerEmail());
    }

    @Test
    void getAccountInformationShowsNoTrainerWhenUnassigned() {

        saveUser("user@example.com", UserRole.USER);

        AccountInformation information =
                accountSettingsService.getAccountInformation(
                        "user@example.com"
                );

        assertEquals("USER", information.getRole());
        assertEquals(
                "No trainer currently assigned",
                information.getAssignedTrainerName()
        );
        assertEquals(
                "Not applicable",
                information.getAssignedTrainerEmail()
        );
    }

    @Test
    void getAccountInformationWorksForTrainerAndAdmin() {

        saveUser("trainer@example.com", UserRole.TRAINER);
        saveUser("admin@example.com", UserRole.ADMIN);

        AccountInformation trainerInformation =
                accountSettingsService.getAccountInformation(
                        "trainer@example.com"
                );

        AccountInformation adminInformation =
                accountSettingsService.getAccountInformation(
                        "admin@example.com"
                );

        assertEquals("TRAINER", trainerInformation.getRole());
        assertEquals("ADMIN", adminInformation.getRole());

        assertEquals(
                "Not applicable",
                trainerInformation.getAssignedTrainerName()
        );
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {

        saveUser("user@example.com", UserRole.USER);

        PasswordChangeForm form = createPasswordForm(
                "WrongPassword123",
                "NewPassword123",
                "NewPassword123"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.changePassword(
                        "user@example.com", form
                )
        );
    }

    @Test
    void changePasswordRejectsMismatchedPasswords() {

        saveUser("user@example.com", UserRole.USER);

        PasswordChangeForm form = createPasswordForm(
                TEST_PASSWORD,
                "NewPassword123",
                "DifferentPassword123"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.changePassword(
                        "user@example.com", form
                )
        );
    }

    @Test
    void changePasswordRejectsReusingCurrentPassword() {

        saveUser("user@example.com", UserRole.USER);

        PasswordChangeForm form = createPasswordForm(
                TEST_PASSWORD,
                TEST_PASSWORD,
                TEST_PASSWORD
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountSettingsService.changePassword(
                        "user@example.com", form
                )
        );
    }

    @Test
    void changePasswordSuccessfullyStoresBcryptHash() {

        saveUser("user@example.com", UserRole.USER);

        String newPassword = "NewStrongPass456";

        PasswordChangeForm form = createPasswordForm(
                TEST_PASSWORD,
                newPassword,
                newPassword
        );

        accountSettingsService.changePassword(
                "user@example.com", form
        );

        AppUser updated =
                userRepository.findByEmail("user@example.com").orElseThrow();

        assertNotEquals(newPassword, updated.getPassword());
        assertTrue(passwordEncoder.matches(
                newPassword, updated.getPassword()
        ));
        assertFalse(passwordEncoder.matches(
                TEST_PASSWORD, updated.getPassword()
        ));
    }

    private ProfileUpdateForm createProfileForm(
            String firstName,
            String lastName,
            String email) {

        ProfileUpdateForm form = new ProfileUpdateForm();

        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);

        return form;
    }

    private PasswordChangeForm createPasswordForm(
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        PasswordChangeForm form = new PasswordChangeForm();

        form.setCurrentPassword(currentPassword);
        form.setNewPassword(newPassword);
        form.setConfirmPassword(confirmPassword);

        return form;
    }

    private AppUser saveUser(String email, UserRole role) {

        AppUser user = new AppUser();

        user.setFirstName("Test");
        user.setLastName("Account");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);

        return userRepository.save(user);
    }
}