package momo.steps.momosteps.core.entity;



import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "daily_steps")
public class DailySteps {
    @Id
    private String id;
    private String userId;
    private LocalDate date;
    private int totalSteps;

    private boolean isRankingProcessed;

    public DailySteps(String userId, LocalDate date, int totalSteps) {
        this.userId = userId;
        this.date = date;
        this.totalSteps = totalSteps;
    }

    public DailySteps(String userId, int totalSteps) {
        this.userId = userId;
        this.totalSteps = totalSteps;
    }
}
