package momo.steps.momosteps.api.service;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.repository.DailyStepsRepository;
import momo.steps.momosteps.core.repository.UserRepository;
import momo.steps.momosteps.model.UserRankingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.ReactiveBulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyStepsService {
    @Autowired
    private  RedisTemplate<String, String> redisTemplate;
    @Autowired
    private  ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private  UserRepository userRepository;

    @Autowired
    private DailyStepsRepository dailyStepsRepository;

    public void batchUpdateDailyStepsForUsers(Map<String, Integer> userStepsMap, LocalDate date) {
        List<DailySteps> dailyStepDataList = userStepsMap.entrySet().stream()
                .map(entry -> new DailySteps(entry.getKey(), date, entry.getValue()))
                .collect(Collectors.toList());

        ReactiveBulkOperations bulkOperations = reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DailySteps.class);
        dailyStepDataList.forEach(dailyStep -> {
            Query query = new Query(Criteria.where("userId").is(dailyStep.getUserId()).and("date").is(date));
            Update update = new Update()
                    .set("totalSteps", dailyStep.getTotalSteps())
                    .set("isRankingProcessed", false);
            bulkOperations.upsert(query, update);
        });

        bulkOperations.execute().subscribe();
    }
}