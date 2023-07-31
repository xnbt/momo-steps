package momo.steps.momosteps.api.service;

import momo.steps.momosteps.core.entity.DailySteps;
import momo.steps.momosteps.core.entity.UserRanking;
import momo.steps.momosteps.core.repository.DailyStepsRepository;
import momo.steps.momosteps.core.repository.UserRankingRepository;
import momo.steps.momosteps.model.UserRankingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveBulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingStepsService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private final MongoTemplate mongoTemplate;
    private final DailyStepsRepository dailyStepsRepository;
    private final UserRankingRepository userRankingRepository;

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;


    public RankingStepsService(MongoTemplate mongoTemplate, DailyStepsRepository dailyStepsRepository,
                               UserRankingRepository userRankingRepository,
                               ReactiveMongoTemplate reactiveMongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.dailyStepsRepository = dailyStepsRepository;
        this.userRankingRepository = userRankingRepository;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public Mono<Object> calculateUserRanking(LocalDate date) {
        int pageNumber = 0;
        int pageSize = 1000;
       return  processPage(pageNumber, pageSize, date)
                .then(Mono.defer(() -> {
                           updateLeaderboardCache(getLeaderboardFromDatabase());
                    return Mono.empty();
                }));
    }

    private Mono<Void> processPage(int pageNumber, int pageSize, LocalDate date) {
        // Lấy danh sách user và số bước chân của họ trong trang hiện tại
        return findUsersAndStepsByPageOrByTotalStep(date, pageNumber, pageSize)
                .collectList()
                .flatMap(userStepsList -> {
                    // Tính toán xếp hạng dựa trên số bước chân của mỗi user
                    List<UserRanking> userRankingList = calculateRanking(userStepsList);

                    // Lưu thông tin xếp hạng vào bảng user_ranking
                    return userRankingRepository.saveAll(userRankingList)
                            .then(Mono.defer(() -> {
                                if (userStepsList.size() < pageSize) {
                                    // Nếu kích thước trang hiện tại nhỏ hơn pageSize, tức là đã xử lý xong toàn bộ dữ liệu
                                    return Mono.empty();
                                } else {
                                    // Nếu còn dữ liệu, tiếp tục xử lý trang tiếp theo
                                    return processPage(pageNumber + 1, pageSize, date)
                                            .then(Mono.defer(() -> markRankingProcessed(userStepsList).then()));
                                }
                            }));
                });
    }

    private List<UserRanking> calculateRanking(List<DailySteps> userStepsList) {
        // Sắp xếp danh sách userStepsList theo số bước chân giảm dần
        Collections.sort(userStepsList, Comparator.comparingInt(DailySteps::getTotalSteps).reversed());

        List<UserRanking> userRankingList = new ArrayList<>();
        int ranking = 1;
        for (DailySteps userSteps : userStepsList) {
            UserRanking userRanking = new UserRanking();
            userRanking.setUserId(userSteps.getUserId());
            userRanking.setTotalSteps(userSteps.getTotalSteps());
            userRanking.setRanking(ranking);

            userRankingList.add(userRanking);
            ranking++;
        }

        return userRankingList;
    }

    public Flux<DailySteps> findUsersAndStepsByPageOrByTotalStep(LocalDate date, int pageNumber, int pageSize) {
        Query query = new Query(Criteria.where("date").is(date));
        query.with(PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "steps")));
        query.fields().include("userId").include("steps");
        query.limit(pageSize);

        return reactiveMongoTemplate.find(query, DailySteps.class)
                .map(dailyStep -> new DailySteps(dailyStep.getUserId(), dailyStep.getTotalSteps()));
    }

    public Mono<Void> markRankingProcessed(List<DailySteps> userStepsList) {
        ReactiveBulkOperations bulkOperations = reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DailySteps.class);
        userStepsList.forEach(userSteps -> {
            Query query = new Query(Criteria.where("userId").is(userSteps.getUserId()).and("date").is(userSteps.getDate()));
            Update update = new Update().set("ranking_processed", true);
            bulkOperations.updateOne(query, update);
        });

        return bulkOperations.execute()
                .then();
    }

    public Flux<UserRankingDTO> getLeaderboard() {
        ReactiveZSetOperations<String, String> zSetOps = reactiveRedisTemplate.opsForZSet();

        // Kiểm tra xem cache bảng xếp hạng có tồn tại không
        return zSetOps.size("leaderboard")
                .flatMapMany(size -> {
                    if (size > 0) {
                        // Nếu cache tồn tại, truy vấn danh sách người dùng hàng đầu với số bước chân cao nhất
                        return zSetOps.reverseRangeByScoreWithScores("leaderboard", Range.unbounded(), Limit.limit().count(1000))
                                .map(tuple -> {
                                    String userId = tuple.getValue();
                                    double totalSteps = tuple.getScore();
                                    UserRankingDTO ranking = new UserRankingDTO();
                                    ranking.setUserId(userId);
                                    ranking.setTotalSteps((long) totalSteps);
                                    return ranking;
                                });
                    } else {
                        // Nếu cache không tồn tại, tính toán xếp hạng và cập nhật cache
                        LocalDate currentDate = LocalDate.now();
                        return calculateUserRanking(currentDate).thenMany(Flux.empty());
                    }
                });
    }

    private Flux<UserRankingDTO> getLeaderboardFromDatabase() {
        int pageNumber = 0;
        int pageSize = 1000;

        Flux<DailySteps> topUsers = findUsersAndStepsByPageOrByTotalStep(LocalDate.now(), pageNumber, pageSize);

        return topUsers
                .collectSortedList(Comparator.comparingInt(DailySteps::getTotalSteps).reversed())
                .flatMapMany(stepsList -> Flux.fromIterable(stepsList)
                        .map(dailySteps -> new UserRankingDTO(dailySteps.getUserId(), dailySteps.getTotalSteps())));
    }

    private Mono<Void> updateLeaderboardCache(Flux<UserRankingDTO> leaderboard) {
        return leaderboard.collectList()
                .flatMap(leaderboardList -> {
                    if (!leaderboardList.isEmpty()) {
                        Set<ZSetOperations.TypedTuple<String>> typedTuples = leaderboardList.stream()
                                .map(userRankingDTO -> new DefaultTypedTuple<>(userRankingDTO.getUserId(), Double.valueOf(userRankingDTO.getTotalSteps())))
                                .collect(Collectors.toSet());

                        return reactiveRedisTemplate.opsForZSet().addAll("leaderboard", typedTuples)
                                .then();
                    }
                    return Mono.empty();
                });
    }
}


