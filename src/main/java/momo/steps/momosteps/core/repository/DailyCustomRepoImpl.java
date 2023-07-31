package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Map;

public class DailyCustomRepoImpl<T> implements DailyStepCustomRepo<T> {

    public Flux<DailySteps> findByUserIdInAndDate(String[] userIds, String date){
        return null;
    }
}