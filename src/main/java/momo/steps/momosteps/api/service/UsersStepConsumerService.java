package momo.steps.momosteps.api.service;

import momo.steps.momosteps.model.UserStepData;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsersStepConsumerService {

    private final DailyStepsService dailyStepsService;

    public UsersStepConsumerService(DailyStepsService dailyStepsService) {
        this.dailyStepsService = dailyStepsService;
    }

    @KafkaListener(topics = "user-steps-topic", containerFactory = "kafkaListenerContainerFactory")
    public void consumeUserSteps(List<UserStepData> userStepDataList) {
        // consider about Synchronous Message Handle and Asynchronous Message Handle
        try {
            dailyStepsService.batchUpdateDailyStepsForUsers(getUserStepsMap(userStepDataList), getDate(userStepDataList));
        } catch (Exception e) {
            // handle when batch update failure
        }
    }

    private Map<String, Integer> getUserStepsMap(List<UserStepData> userStepDataList) {
        return new HashMap<>();
    }

    private LocalDate getDate(List<UserStepData> userStepDataList) {
        return null;
    }

}

