package momo.steps.momosteps.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRankingDTO {
    private String id;
    private String userId;
    private long totalSteps;
    private LocalDate updatedAt;

    public UserRankingDTO(String userId, long totalSteps) {
        this.userId = userId;
        this.totalSteps = totalSteps;
    }

    public UserRankingDTO() {
    }
}
