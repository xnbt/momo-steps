package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyStepsRepository extends ReactiveMongoRepository<DailySteps, String> {
}