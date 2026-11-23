package com.group19.calorietrack.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSettingsControllerTest {

    private static final String TEST_PASSWORD = "StrongPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createTestAccounts() {

        userRepository.deleteAll();

        saveUser("user@example.com", UserRole.USER);
        saveUser("trainer@example.com", UserRole.TRAINER);
        saveUser("admin@example.com", UserRole.ADMIN);
    }

    @AfterEach
    void removeTestAccounts() {
        userRepository.deleteAll();
    }

    @Test
    void ACC01_regularUserCanViewAccountSettings() throws Exception {

        mockMvc.perform(get("/account/settings")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"))
                .andExpect(model().attributeExists("profileForm"))
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().attributeExists("accountInformation"));
    }

    @Test
    void ACC02_trainerCanViewAccountSettings() throws Exception {

        mockMvc.perform(get("/account/settings")
                        .with(user("trainer@example.com").roles("TRAINER")))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"));
    }

    @Test
    void ACC03_adminCanViewAccountSettings() throws Exception {

        mockMvc.perform(get("/account/settings")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"));
    }

    @Test
    void ACC04_unauthenticatedUserCannotViewSettings() throws Exception {

        mockMvc.perform(get("/account/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void ACC05_userCanUpdateProfileSuccessfully() throws Exception {

        mockMvc.perform(post("/account/settings")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/settings"))
                .andExpect(flash().attributeExists("successMessage"));

        AppUser updated =
                userRepository.findByEmail("user@example.com").orElseThrow();

        assertEquals("Alex", updated.getFirstName());
        assertEquals("Morgan", updated.getLastName());
    }

    @Test
    void ACC06_userCanChangeEmailSuccessfully() throws Exception {

        mockMvc.perform(post("/account/settings")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "new.user@example.com")
                        .param("currentPassword", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailChanged"))
                .andExpect(flash().attributeExists("successMessage"));

        assertTrue(userRepository.findByEmail("user@example.com").isEmpty());
        assertTrue(userRepository.findByEmail("new.user@example.com").isPresent());
    }

    @Test
    void ACC07_duplicateEmailIsRejected() throws Exception {

        mockMvc.perform(post("/account/settings")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "trainer@example.com")
                        .param("currentPassword", TEST_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"))
                .andExpect(model().attributeExists("accountInformation"));

        assertTrue(userRepository.findByEmail("user@example.com").isPresent());
    }

    @Test
    void ACC08_userCanChangePasswordSuccessfully() throws Exception {

        String newPassword = "NewStrongPass456";

        mockMvc.perform(post("/account/settings/password")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", TEST_PASSWORD)
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged"))
                .andExpect(flash().attributeExists("successMessage"));

        AppUser updated =
                userRepository.findByEmail("user@example.com").orElseThrow();

        assertTrue(passwordEncoder.matches(
                newPassword, updated.getPassword()));

        assertFalse(passwordEncoder.matches(
                TEST_PASSWORD, updated.getPassword()));
    }

    @Test
    void ACC09_incorrectCurrentPasswordIsRejected() throws Exception {

        mockMvc.perform(post("/account/settings/password")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", "WrongPassword123")
                        .param("newPassword", "NewStrongPass456")
                        .param("confirmPassword", "NewStrongPass456"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"));

        AppUser userAccount =
                userRepository.findByEmail("user@example.com").orElseThrow();

        assertTrue(passwordEncoder.matches(
                TEST_PASSWORD, userAccount.getPassword()));
    }

    @Test
    void ACC10_mismatchedNewPasswordsAreRejected() throws Exception {

        mockMvc.perform(post("/account/settings/password")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", TEST_PASSWORD)
                        .param("newPassword", "NewStrongPass456")
                        .param("confirmPassword", "DifferentPassword456"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings"));
    }

    @Test
    void ACC11_missingCsrfTokenIsRejected() throws Exception {

        mockMvc.perform(post("/account/settings")
                        .with(user("user@example.com").roles("USER"))
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "user@example.com"))
                .andExpect(status().isForbidden());
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