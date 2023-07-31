package momo.steps.momosteps.api.controller;

import momo.steps.momosteps.api.service.DailyStepsService;
import momo.steps.momosteps.api.service.MonthlyStepsService;
import momo.steps.momosteps.api.service.RankingStepsService;
import momo.steps.momosteps.api.service.WeeklyStepsService;
import momo.steps.momosteps.model.UserRankingDTO;
import momo.steps.momosteps.model.UserStepData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Autowired
    private DailyStepsService dailyStepsService;

    @Autowired
    private WeeklyStepsService weeklyStepsService;

    @Autowired
    private MonthlyStepsService monthlyStepsService;

    @Autowired
    private  KafkaTemplate<String, UserStepData> kafkaTemplate;

    @Autowired
    RankingStepsService rankingStepsService;

    @PostMapping("/steps/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> recordSteps(@PathVariable String userId, @RequestBody int steps) {
        // send message Kafka topic "user_steps_topic"
        // consider about Synchronous Message Sending and Asynchronous Message Sending
        UserStepData userStepData = new UserStepData(userId, LocalDate.now(), steps);
        kafkaTemplate.send("user_steps_topic", userStepData);

        return Mono.empty();
    }

    @GetMapping("/weekly-steps/{userId}")
    public Mono<Integer> getWeeklySteps(@PathVariable String userId) {
        return weeklyStepsService.getWeeklySteps(userId);
    }

    @GetMapping("/monthly-steps/{userId}")
    public Mono<Long> getMonthlySteps(@PathVariable String userId) {
        return monthlyStepsService.getMonthlySteps(userId);
    }

    @GetMapping("/leaderboard")
    public Flux<UserRankingDTO> getLeaderboard() {
        return rankingStepsService.getLeaderboard();
    }
}