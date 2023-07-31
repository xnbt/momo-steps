package momo.steps.momosteps.api.service;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.entity.MonthlySteps;
import momo.steps.momosteps.core.entity.WeeklySteps;
import momo.steps.momosteps.core.repository.DailyStepsRepository;
import momo.steps.momosteps.core.repository.MonthlyStepsRepository;
import momo.steps.momosteps.core.repository.WeeklyStepsRepository;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.ReactiveBulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MonthlyStepsService {

    private final WeeklyStepsRepository weeklyStepsRepository;
    private final MonthlyStepsRepository monthlyStepsRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final int BATCH_SIZE = 1000;

    public MonthlyStepsService(WeeklyStepsRepository weeklyStepsRepository, MonthlyStepsRepository monthlyStepsRepository,
                               ReactiveMongoTemplate reactiveMongoTemplate) {
        this.weeklyStepsRepository = weeklyStepsRepository;
        this.monthlyStepsRepository = monthlyStepsRepository;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public void batchUpdateMonthlyStepsFromWeeklySteps(LocalDate date) {
        Flux<WeeklySteps> weeklyStepsFlux = weeklyStepsRepository.findByYearAndMonthAndNotMonthlyProcessed(date.getYear(), date.getMonthValue());
        weeklyStepsFlux
                .buffer(BATCH_SIZE)
                .doOnNext(weeklyStepsList -> {
                    List<MonthlySteps> monthlyStepsList = calculateMonthlySteps(weeklyStepsList);
                    batchUpdateMonthlySteps(monthlyStepsList).subscribe();
                })
                .blockLast();
    }

    private Mono<Void> batchUpdateMonthlySteps(List<MonthlySteps> monthlyStepsList) {
        ReactiveBulkOperations bulkOperations = reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MonthlySteps.class);
        monthlyStepsList.forEach(monthlySteps -> {
            Query query = new Query(Criteria.where("userId").is(monthlySteps.getUserId())
                    .and("year").is(monthlySteps.getYear())
                    .and("month").is(monthlySteps.getMonth()));
            Update update = new Update()
                    .set("totalSteps", monthlySteps.getTotalSteps())
                    .set("monthly_processed", true)
                    .set("calculationDate", LocalDate.now());
            bulkOperations.updateOne(query, update);
        });

        return bulkOperations.execute().then();
    }

    private List<MonthlySteps> calculateMonthlySteps(List<WeeklySteps> weeklyStepsList) {
        Map<String, Integer> userMonthlyStepsMap = weeklyStepsList.stream()
                .collect(Collectors.groupingBy(WeeklySteps::getUserId, Collectors.summingInt(WeeklySteps::getTotalSteps)));

        int month = weeklyStepsList.get(0).getMonth();
        int year = weeklyStepsList.get(0).getYear();

        return userMonthlyStepsMap.entrySet().stream()
                .map(entry -> new MonthlySteps(entry.getKey(), year, month, entry.getValue()))
                .collect(Collectors.toList());
    }

    public Mono<Long> getMonthlySteps(String userId) {
        LocalDate startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        AggregationOperation match = Aggregation.match(Criteria.where("userId").is(userId).and("date").gte(startOfMonth).lte(endOfMonth));
        AggregationOperation group = Aggregation.group().sum("steps").as("totalSteps");

        Aggregation aggregation = Aggregation.newAggregation(match, group);
        return reactiveMongoTemplate.aggregate(aggregation, DailySteps.class, MonthlySteps.class)
                .map(MonthlySteps::getTotalSteps)
                .next()
                .defaultIfEmpty(0L);
    }
}


