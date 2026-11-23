package com.group19.calorietrack.account;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group19.calorietrack.user.UserRole;

@Service
public class AccountSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountSettingsService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileUpdateForm getProfile(String authenticatedEmail) {

        AppUser user = findUser(authenticatedEmail);

        ProfileUpdateForm form = new ProfileUpdateForm();

        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());

        return form;
    }

    @Transactional
    public boolean updateProfile(
            String authenticatedEmail,
            ProfileUpdateForm form) {

        AppUser user = findUser(authenticatedEmail);

        String firstName = form.getFirstName().trim();
        String lastName = form.getLastName().trim();
        String newEmail = form.getEmail().trim().toLowerCase();

        boolean emailChanged = !newEmail.equals(user.getEmail());

        if (emailChanged) {

            String currentPassword = form.getCurrentPassword();

            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException(
                        "Enter your current password to change your email.");
            }

            if (!passwordEncoder.matches(
                    currentPassword, user.getPassword())) {

                throw new IllegalArgumentException(
                        "Your current password is incorrect...");
            }

            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException(
                        "An account with this email already exists...");
            }
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (emailChanged) {
            user.setEmail(newEmail);
        }

        userRepository.save(user);

        return emailChanged;
    }

    @Transactional(readOnly = true)
        public AccountInformation getAccountInformation(String authenticatedEmail) {

            AppUser user = findUser(authenticatedEmail);

            String trainerName = "Not applicable";
            String trainerEmail = "Not applicable";

            if (user.getRole() == UserRole.USER) {

                AppUser trainer = user.getAssignedTrainer();

                if (trainer == null) {
                    trainerName = "No trainer currently assigned";
                    trainerEmail = "Not applicable";
                } else {
                    trainerName = trainer.getFirstName() + " " + trainer.getLastName();
                    trainerEmail = trainer.getEmail();
                }
            }

            return new AccountInformation(
                    user.getRole().name(),
                    trainerName,
                    trainerEmail
            );
        }

    @Transactional
        public void changePassword(
                String authenticatedEmail,
                PasswordChangeForm form) {

            AppUser user = findUser(authenticatedEmail);

            String currentPassword = form.getCurrentPassword();
            String newPassword = form.getNewPassword();
            String confirmPassword = form.getConfirmPassword();

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException(
                        "Your current password is incorrect.");
            }

            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException(
                        "The new passwords do not match.");
            }

            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new IllegalArgumentException(
                        "Your new password must be different from your current password.");
            }

            user.setPassword(passwordEncoder.encode(newPassword));

            userRepository.save(user);
        }

    private AppUser findUser(String email) {

        return userRepository.findByEmail(
                email.trim().toLowerCase()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "The requested account could not be found."));
    }
}