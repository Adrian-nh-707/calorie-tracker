package com.group19.calorietrack.account;

public class AccountInformation {

    private final String role;
    private final String assignedTrainerName;
    private final String assignedTrainerEmail;

    public AccountInformation(
            String role,
            String assignedTrainerName,
            String assignedTrainerEmail) {

        this.role = role;
        this.assignedTrainerName = assignedTrainerName;
        this.assignedTrainerEmail = assignedTrainerEmail;
    }

    public String getRole() {
        return role;
    }

    public String getAssignedTrainerName() {
        return assignedTrainerName;
    }

    public String getAssignedTrainerEmail() {
        return assignedTrainerEmail;
    }
}