package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.entity.WeeklySteps;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface WeeklyStepsRepository extends ReactiveMongoRepository<DailySteps, String> {
    Flux<WeeklySteps> findByYearAndMonthAndNotMonthlyProcessed(int year, int monthValue);
}