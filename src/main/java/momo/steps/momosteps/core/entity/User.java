package momo.steps.momosteps.core.entity;



import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "user")
public class User {
    @Id
    private String id;

}
