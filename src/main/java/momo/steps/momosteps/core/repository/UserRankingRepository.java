package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.entity.UserRanking;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRankingRepository extends ReactiveMongoRepository<UserRanking, String> {
}