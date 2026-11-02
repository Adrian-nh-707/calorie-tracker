package com.group19.calorietrack.user;

import java.util.Optional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    long countByRoleAndAssignedTrainerIsNull(UserRole role);

    List<AppUser> findByRole(UserRole role);

    List<AppUser> findByAssignedTrainer_IdAndRole(Long trainerId, UserRole role);
}
