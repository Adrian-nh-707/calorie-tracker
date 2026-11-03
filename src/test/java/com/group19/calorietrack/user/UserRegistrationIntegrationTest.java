package com.group19.calorietrack.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void registrationPageLoads() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    void REG01_registerRegularUserSuccessfully() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "alex.user@example.com")
                        .param("password", "StrongPass123")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        AppUser saved = userRepository.findByEmail("alex.user@example.com").orElseThrow();
        assertEquals(UserRole.USER, saved.getRole());
        assertEquals("Alex", saved.getFirstName());
    }

    @Test
    void REG02_registerTrainerSuccessfully() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Taylor")
                        .param("lastName", "Trainer")
                        .param("email", "taylor.trainer@example.com")
                        .param("password", "StrongPass123")
                        .param("role", "TRAINER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        AppUser saved = userRepository.findByEmail("taylor.trainer@example.com").orElseThrow();
        assertEquals(UserRole.TRAINER, saved.getRole());
    }

    @Test
    void REG03_blankFirstNameIsRejected() throws Exception {
        mockMvc.perform(validRegistration("   ", "Morgan", "blank.first@example.com", "StrongPass123", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "firstName"));

        assertTrue(userRepository.findByEmail("blank.first@example.com").isEmpty());
    }

    @Test
    void REG04_blankLastNameIsRejected() throws Exception {
        mockMvc.perform(validRegistration("Alex", " ", "blank.last@example.com", "StrongPass123", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "lastName"));
    }

    @Test
    void REG05_blankEmailIsRejected() throws Exception {
        mockMvc.perform(validRegistration("Alex", "Morgan", "", "StrongPass123", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    @Test
    void REG06_invalidEmailFormatIsRejected() throws Exception {
        mockMvc.perform(validRegistration("Alex", "Morgan", "not-an-email", "StrongPass123", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    @Test
    void REG07_blankPasswordIsRejected() throws Exception {
        mockMvc.perform(validRegistration("Alex", "Morgan", "blank.password@example.com", "", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
    }

    @Test
    void REG08_passwordShorterThanEightCharactersIsRejected() throws Exception {
        mockMvc.perform(validRegistration("Alex", "Morgan", "short.password@example.com", "short7", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
    }

    @Test
    void REG09_passwordLongerThanSeventyTwoCharactersIsRejected() throws Exception {
        String longPassword = "A".repeat(73);

        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "long.password@example.com", longPassword, "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
    }

    @Test
    void REG10_duplicateEmailIsRejected() throws Exception {
        saveUser("existing@example.com", UserRole.USER);

        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "existing@example.com", "StrongPass123", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        assertEquals(1, userRepository.count());
    }

    @Test
    void REG11_blankAccountTypeIsRejectedByFormValidation() throws Exception {
        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "blank.role@example.com", "StrongPass123", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "role"));
    }

    @Test
    void REG12_publicRegistrationCannotCreateAdminAccount() throws Exception {
        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "attempt.admin@example.com", "StrongPass123", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        assertTrue(userRepository.findByEmail("attempt.admin@example.com").isEmpty());
    }

    @Test
    void REG13_passwordIsStoredAsBcryptHash() throws Exception {
        String rawPassword = "StrongPass123";

        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "bcrypt.check@example.com", rawPassword, "USER"))
                .andExpect(status().is3xxRedirection());

        AppUser saved = userRepository.findByEmail("bcrypt.check@example.com").orElseThrow();
        assertNotEquals(rawPassword, saved.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, saved.getPassword()));
    }

    @Test
    void REG14_emailIsNormalizedToLowercase() throws Exception {
        mockMvc.perform(validRegistration(
                        "Alex", "Morgan", "Alex.Morgan@Example.com", "StrongPass123", "USER"))
                .andExpect(status().is3xxRedirection());

        assertTrue(userRepository.findByEmail("alex.morgan@example.com").isPresent());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRegistration(
            String firstName, String lastName, String email, String password, String role) {
        return post("/register")
                .with(csrf())
                .param("firstName", firstName)
                .param("lastName", lastName)
                .param("email", email)
                .param("password", password)
                .param("role", role);
    }

    private AppUser saveUser(String email, UserRole role) {
        AppUser user = new AppUser();
        user.setFirstName("Existing");
        user.setLastName("Account");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("StrongPass123"));
        user.setRole(role);
        return userRepository.save(user);
    }
}
