package com.group19.calorietrack.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityIntegrationTest {

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
    void LOG01_regularUserLoginRedirectsToUserDashboard() throws Exception {
        login("user@example.com", TEST_PASSWORD, "/dashboard");
    }

    @Test
    void LOG02_trainerLoginRedirectsToTrainerDashboard() throws Exception {
        login("trainer@example.com", TEST_PASSWORD, "/trainer/dashboard");
    }

    @Test
    void LOG03_adminLoginRedirectsToAdminDashboard() throws Exception {
        login("admin@example.com", TEST_PASSWORD, "/admin/dashboard");
    }

    @Test
    void LOG04_incorrectPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "user@example.com")
                        .param("password", "WrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void LOG05_unknownEmailIsRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "unknown@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void LOG06_emptyEmailIsRejected() throws Exception {
               mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void LOG07_emptyPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "user@example.com")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void LOG09_logoutRedirectsToLoginWithLogoutFlag() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void LOG10_protectedDashboardRequiresAuthenticationAfterLogout() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // A new request has no authenticated user and must be redirected to login.
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void LOG11_userCanLoginAgainAfterLogout() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify a fresh real login still succeeds after logout.
        login("user@example.com", TEST_PASSWORD, "/dashboard");
    }
    
    @Test
    void SEC01_userCannotAccessTrainerDashboard() throws Exception {
        mockMvc.perform(get("/trainer/dashboard")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void SEC02_userCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void SEC03_trainerCannotAccessUserDashboard() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("trainer@example.com").roles("TRAINER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void SEC04_trainerCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("trainer@example.com").roles("TRAINER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void SEC05_adminCanAccessAdminDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/admin/dashboard")
                        .session(session)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void unauthenticatedUserCannotOpenProtectedPage() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    private void login(String email, String password, String expectedRedirect) throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirect));
    }

    private AppUser saveUser(String email, UserRole role) {
        AppUser account = new AppUser();
        account.setFirstName("Test");
        account.setLastName(role.name());
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        account.setRole(role);
        return userRepository.save(account);
    }
}