package momo.steps.momosteps.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MonthlyStepsScheduler {

    private final MonthlyStepsService monthlyStepsService;

    public MonthlyStepsScheduler(MonthlyStepsService monthlyStepsService) {
        this.monthlyStepsService = monthlyStepsService;
    }

    // Scheduled to run every Monday at 1 AM (CRON expression: "0 1 * * MON")
    @Scheduled(cron = "0 1 * * MON")
    public void calculateAndBatchUpdateWeeklySteps() {
        LocalDate calculationDate = LocalDate.now().minusDays(1); // Calculate for the previous day
        monthlyStepsService.batchUpdateMonthlyStepsFromWeeklySteps(calculationDate);
    }
}