package momo.steps.momosteps.api.service;

import momo.steps.momosteps.api.service.RankingStepsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RankingStepsScheduler {

    private final RankingStepsService rankingStepsService;

    public RankingStepsScheduler(RankingStepsService rankingStepsService) {
        this.rankingStepsService = rankingStepsService;
    }

    // Lên lịch chạy vào 23:59 hàng ngày
    @Scheduled(cron = "0 59 23 * * ?")
    public void calculateUserRankingDaily() {
        LocalDate currentDate = LocalDate.now();
        rankingStepsService.calculateUserRanking(currentDate);
    }
}