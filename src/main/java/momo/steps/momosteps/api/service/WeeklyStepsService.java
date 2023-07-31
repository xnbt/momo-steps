package momo.steps.momosteps.api.service;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.entity.WeeklySteps;
import momo.steps.momosteps.core.repository.WeeklyStepsRepository;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.ReactiveBulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeeklyStepsService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final WeeklyStepsRepository weeklyStepsRepository;

    public WeeklyStepsService(ReactiveMongoTemplate reactiveMongoTemplate, WeeklyStepsRepository weeklyStepsRepository) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.weeklyStepsRepository = weeklyStepsRepository;
    }

    public void batchUpdateWeeklyStepsFromDailySteps(LocalDate calculationDate) {
        Query query = new Query(Criteria.where("processed").is(false));
        query.limit(1000); // Lấy 1000 dòng đầu tiên

        Flux<DailySteps> dailyStepsFlux = reactiveMongoTemplate.find(query, DailySteps.class);

        dailyStepsFlux.collectList().subscribe(dailyStepDataList -> {
            // Thực hiện tính toán và chuyển đổi dữ liệu từ dailyStepDataList sang weeklyStepsList
            List<WeeklySteps> weeklyStepsList = new ArrayList<>();
            Map<String, Map<Integer, Integer>> userWeekStepsMap = new HashMap<>();

            for (DailySteps dailyStep : dailyStepDataList) {
                String userId = dailyStep.getUserId();
                int week = calculateWeek(dailyStep.getDate());
                int steps = dailyStep.getTotalSteps();

                userWeekStepsMap.putIfAbsent(userId, new HashMap<>());
                Map<Integer, Integer> weekStepsMap = userWeekStepsMap.get(userId);
                weekStepsMap.put(week, weekStepsMap.getOrDefault(week, 0) + steps);
            }

            for (Map.Entry<String, Map<Integer, Integer>> entry : userWeekStepsMap.entrySet()) {
                String userId = entry.getKey();
                Map<Integer, Integer> weekStepsMap = entry.getValue();

                for (Map.Entry<Integer, Integer> weekEntry : weekStepsMap.entrySet()) {
                    int week = weekEntry.getKey();
                    int totalSteps = weekEntry.getValue();

                    WeeklySteps weeklyStep = new WeeklySteps(userId, week, totalSteps, LocalDate.now());
                    weeklyStepsList.add(weeklyStep);
                }
            }

            // Batch update bảng weekly_steps
            ReactiveBulkOperations bulkOperations = reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, WeeklySteps.class);
            weeklyStepsList.forEach(weeklyStep -> {
                Query updateQuery = new Query(Criteria.where("userId").is(weeklyStep.getUserId()).and("week").is(weeklyStep.getWeek()));
                Update update = new Update()
                        .set("totalSteps", weeklyStep.getTotalSteps())
                        .set("calculationDate", LocalDate.now());
                bulkOperations.upsert(updateQuery, update);
            });

            bulkOperations.execute().subscribe();

            // Sau khi cập nhật thành công, đánh dấu các bản ghi đã xử lý bằng cách set trường processed thành true
            List<String> processedIds = dailyStepDataList.stream().map(DailySteps::getId).collect(Collectors.toList());
            Update updateProcessed = new Update().set("weekly_processed", true);
            Query updateQueryProcessed = new Query(Criteria.where("id").in(processedIds));
            reactiveMongoTemplate.updateMulti(updateQueryProcessed, updateProcessed, DailySteps.class).subscribe();
        });

    }

    public Mono<Integer> getWeeklySteps(String userId) {
        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY);

        AggregationOperation match = Aggregation.match(Criteria.where("userId").is(userId).and("date").gte(startOfWeek).lte(endOfWeek));
        AggregationOperation group = Aggregation.group().sum("steps").as("totalSteps");

        Aggregation aggregation = Aggregation.newAggregation(match, group);
        return reactiveMongoTemplate.aggregate(aggregation, DailySteps.class, WeeklySteps.class)
                .map(WeeklySteps::getTotalSteps)
                .next()
                .defaultIfEmpty(0);
    }
    private int calculateWeek(LocalDate date) {
        LocalDate firstDayOfYear = date.with(TemporalAdjusters.firstDayOfYear());
        int daysDiff = date.getDayOfYear() - firstDayOfYear.getDayOfYear();
        int week = daysDiff / 7 + 1;
        return week;
    }

    private Mono<Void> updateWeeklySteps(Map<String, Integer> userStepsMap) {
        List<WeeklySteps> weeklyStepsList = userStepsMap.entrySet().stream()
                .map(entry -> new WeeklySteps(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        ReactiveBulkOperations bulkOperations = reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, WeeklySteps.class);
        weeklyStepsList.forEach(weeklySteps -> {
            Query query = new Query(Criteria.where("userId").is(weeklySteps.getUserId()));
            Update update = new Update().inc("totalSteps", weeklySteps.getTotalSteps());
            bulkOperations.upsert(query, update);
        });

        return bulkOperations.execute().then();
    }
}

