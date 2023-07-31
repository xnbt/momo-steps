package momo.steps.momosteps.core.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user_ranking")
public class UserRanking {

    @Id
    private String id;
    private String userId;
    private long totalSteps;
    private int ranking;
}
