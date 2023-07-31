package momo.steps.momosteps.core.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "monthly_steps")
public class MonthlySteps {

    @Id
    private String id;
    private String userId;
    private int year;
    private int month;
    private long totalSteps;

    public MonthlySteps(String userId, int year, int month, long totalSteps) {
        this.userId = userId;
        this.year = year;
        this.month = month;
        this.totalSteps = totalSteps;
    }
}
