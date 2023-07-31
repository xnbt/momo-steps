package momo.steps.momosteps.core.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "weekly_steps")
public class WeeklySteps {

    @Id
    private String id;
    private String userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalSteps;
    private int week;

    private boolean monthlyProcessed;

    public WeeklySteps(String userId, int totalSteps) {
        this.userId = userId;
        this.totalSteps = totalSteps;
    }

    public WeeklySteps(String userId, int week, int totalSteps, LocalDate startDate) {
        this.userId = userId;
        this.totalSteps = totalSteps;
        this.week = week;
        this.startDate = startDate;
    }

    public int getMonth() {
        return startDate.getMonth().getValue();
    }

    public int getYear() {
        return startDate.getYear();
    }
}

