package momo.steps.momosteps.core.repository;

import momo.steps.momosteps.core.entity.DailySteps;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface DailyStepCustomRepo<T>{
    Flux<DailySteps> findByUserIdInAndDate(String[] userIds, String date);
}
