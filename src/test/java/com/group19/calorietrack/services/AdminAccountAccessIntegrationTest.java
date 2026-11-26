package com.group19.calorietrack.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdminAccountAccessIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String USER_EMAIL = "user@example.com";
    private static final String TRAINER_EMAIL = "trainer@example.com";
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

        saveAccount(ADMIN_EMAIL, UserRole.ADMIN);
        saveAccount(USER_EMAIL, UserRole.USER);
        saveAccount(TRAINER_EMAIL, UserRole.TRAINER);
    }

    @AfterEach
    void removeTestAccounts() {
        userRepository.deleteAll();
    }

    @Test
    void ADM01_adminCanDeactivateUserAccount() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        mockMvc.perform(post("/admin/deactivate-account")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", userAccount.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        AppUser updated =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        assertFalse(updated.isEnabled());
    }

    @Test
    void ADM02_adminCanReactivateUserAccount() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        userAccount.setEnabled(false);
        userRepository.save(userAccount);

        mockMvc.perform(post("/admin/reactivate-account")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", userAccount.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        AppUser updated =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        assertTrue(updated.isEnabled());
    }

    @Test
    void ADM03_nonAdminCannotDeactivateAccounts() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        mockMvc.perform(post("/admin/deactivate-account")
                        .with(user(USER_EMAIL).roles("USER"))
                        .with(csrf())
                        .param("userId", userAccount.getId().toString()))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.findByEmail(USER_EMAIL)
                .orElseThrow().isEnabled());
    }

    @Test
    void ADM04_unauthenticatedUserCannotManageAccounts() throws Exception {

        mockMvc.perform(post("/admin/deactivate-account")
                        .with(csrf())
                        .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void ADM05_adminCannotDeactivateAdminAccount() throws Exception {

        AppUser adminAccount =
                userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(post("/admin/deactivate-account")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", adminAccount.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertTrue(userRepository.findByEmail(ADMIN_EMAIL)
                .orElseThrow().isEnabled());
    }

    @Test
    void ADM06_disabledUserCannotLogin() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        userAccount.setEnabled(false);
        userRepository.save(userAccount);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USER_EMAIL)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void ADM07_reactivatedUserCanLoginAgain() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        userAccount.setEnabled(false);
        userRepository.save(userAccount);

        // Administrator reactivates the account
        mockMvc.perform(post("/admin/reactivate-account")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", userAccount.getId().toString()))
                .andExpect(status().is3xxRedirection());

        // Verify that the user can now log in
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USER_EMAIL)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void ADM08_missingCsrfTokenPreventsDeactivation() throws Exception {

        AppUser userAccount =
                userRepository.findByEmail(USER_EMAIL).orElseThrow();

        mockMvc.perform(post("/admin/deactivate-account")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .param("userId", userAccount.getId().toString()))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.findByEmail(USER_EMAIL)
                .orElseThrow().isEnabled());
    }

    private AppUser saveAccount(String email, UserRole role) {

        AppUser account = new AppUser();

        account.setFirstName("Test");
        account.setLastName("Account");
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        account.setRole(role);
        account.setEnabled(true);

        return userRepository.save(account);
    }
}