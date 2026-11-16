
package com.group19.calorietrack.models;

import java.time.LocalDateTime;

import com.group19.calorietrack.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "trainer_goal_recommendations")
public class TrainerGoalRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Trainer that created the recommendation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private AppUser trainer;


    @ManyToOne(fetch = FetchType.LAZY, optional = false) // User receiving the recommendation
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "calorie_goal", nullable = false)
    private int calorieGoal;

    @Column(name = "protein_goal", nullable = false)
    private int proteinGoal;

    @Column(length = 1000)
    private String note;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TrainerGoalRecommendation() {
    }

    public TrainerGoalRecommendation(
            AppUser trainer,
            AppUser user,
            int calorieGoal,
            int proteinGoal,
            String note) {

        this.trainer = trainer;
        this.user = user;
        this.calorieGoal = calorieGoal;
        this.proteinGoal = proteinGoal;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AppUser getTrainer() {
        return trainer;
    }

    public void setTrainer(AppUser trainer) {
        this.trainer = trainer;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public int getCalorieGoal() {
        return calorieGoal;
    }

    public void setCalorieGoal(int calorieGoal) {
        this.calorieGoal = calorieGoal;
    }

    public int getProteinGoal() {
        return proteinGoal;
    }

    public void setProteinGoal(int proteinGoal) {
        this.proteinGoal = proteinGoal;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
