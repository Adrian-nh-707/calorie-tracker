package com.group19.calorietrack.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerUser(
            String firstName,
            String lastName,
            String email,
            String rawPassword,
            UserRole role) {

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        AppUser user = new AppUser();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        
        // store encoded password
        user.setPassword(passwordEncoder.encode(rawPassword));

        if (role != UserRole.USER && role != UserRole.TRAINER) {
            throw new IllegalArgumentException("Please select a valid account type.");
        }

        user.setRole(role);

        return userRepository.save(user);
    }
}