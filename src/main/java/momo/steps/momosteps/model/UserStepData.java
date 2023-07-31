package momo.steps.momosteps.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserStepData {
    private String userId;
    private LocalDate date;
    private int steps;

    public UserStepData() {
    }

    public UserStepData(String userId, LocalDate date, int steps) {
        this.userId = userId;
        this.date = date;
        this.steps = steps;
    }

    public UserStepData(String userId, int i) {
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    // toString() method

    @Override
    public String toString() {
        return "UserStepData{" +
                "userId='" + userId + '\'' +
                ", date=" + date +
                ", steps=" + steps +
                '}';
    }
}

