package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface DailyStepsRepository extends ReactiveMongoRepository<DailySteps, String>, DailyStepCustomRepo<DailySteps> {
    Flux<DailySteps> findByUserIdInAndDate(String[] userIds, String date);


    Flux<DailySteps> findUsersAndStepsOrderByStep(LocalDate date, int pageNumber, int pageSize);
}