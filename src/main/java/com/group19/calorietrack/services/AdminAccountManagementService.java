package com.group19.calorietrack.services;

import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountManagementService {

    private final UserRepository userRepository;

    public AdminAccountManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void changeUserRole(Long userId, UserRole newRole) {
        if (newRole != UserRole.USER && newRole != UserRole.TRAINER) {
            throw new IllegalArgumentException("Administrators cannot be assigned through this feature.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found..."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be changed here.");
        }

        if (user.getRole() == newRole) {
            throw new IllegalArgumentException("This account already has the selected role.");
        }

        if (user.getRole() == UserRole.TRAINER && newRole == UserRole.USER) {
            long assignedUsers = userRepository.countByAssignedTrainer_IdAndRole(user.getId(), UserRole.USER);

            if (assignedUsers > 0) {
                throw new IllegalArgumentException("Reassign or unassign this trainer's users before changing their role.");
            }
        }

        if (user.getRole() == UserRole.USER && newRole == UserRole.TRAINER) {
            user.setAssignedTrainer(null);
        }

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(Long userId, String administratorEmail) {

        AppUser administrator = userRepository.findByEmail(administratorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Administrator account not found..."));

        if (administrator.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can manage account access.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found..."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be deactivated.");
        }

        if (user.getId().equals(administrator.getId())) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }

        if (user.getRole() == UserRole.TRAINER) {
            long assignedUsers = userRepository.countByAssignedTrainer_IdAndRole(user.getId(), UserRole.USER);

            if (assignedUsers > 0) {
                throw new IllegalArgumentException("Reassign or unassign this trainer's users before deactivating the account.");
            }
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("This account is already inactive...");
        }

        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void reactivateAccount(Long userId, String administratorEmail) {

        AppUser administrator = userRepository.findByEmail(administratorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Administrator account not found..."));

        if (administrator.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can manage account access.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found..."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be managed here.");
        }

        if (user.isEnabled()) {
            throw new IllegalArgumentException("This account is already active.");
        }

        user.setEnabled(true);
        userRepository.save(user);
    }
}