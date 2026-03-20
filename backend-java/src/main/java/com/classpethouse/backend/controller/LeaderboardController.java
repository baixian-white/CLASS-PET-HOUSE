package com.classpethouse.backend.controller;

import com.classpethouse.backend.dto.ClassLeaderboardsDto;
import com.classpethouse.backend.dto.LeaderboardFoodSummaryDto;
import com.classpethouse.backend.dto.LeaderboardStudentItemDto;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.StudentTotalFoodSummaryEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.service.StudentRankingStatsService;
import com.classpethouse.backend.util.AuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/leaderboards")
public class LeaderboardController {

    private final StudentRepository studentRepository;
    private final AppSupportService appSupportService;
    private final StudentRankingStatsService studentRankingStatsService;

    public LeaderboardController(
            StudentRepository studentRepository,
            AppSupportService appSupportService,
            StudentRankingStatsService studentRankingStatsService
    ) {
        this.studentRepository = studentRepository;
        this.appSupportService = appSupportService;
        this.studentRankingStatsService = studentRankingStatsService;
    }

    @GetMapping("/class/{classId}")
    public ClassLeaderboardsDto listByClass(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());

        // 榜单读取直接使用 students 上的持久化统计字段，避免每次切榜都全量扫描 history。
        List<StudentEntity> currentFoodRanking = studentRepository.findByClassIdOrderByFoodCountDescCreatedAtAscIdAsc(classId);
        List<StudentEntity> totalFoodRanking = studentRepository.findByClassIdOrderByTotalFoodEarnedDescCreatedAtAscIdAsc(classId);
        List<StudentEntity> currentBadgesRanking = studentRepository.findByClassIdOrderByCurrentBadgesCountDescCreatedAtAscIdAsc(classId);
        List<StudentEntity> totalBadgesRanking = studentRepository.findByClassIdOrderByTotalBadgesEarnedDescCreatedAtAscIdAsc(classId);

        appSupportService.populateGroups(currentFoodRanking);
        appSupportService.populateGroups(totalFoodRanking);
        appSupportService.populateGroups(currentBadgesRanking);
        appSupportService.populateGroups(totalBadgesRanking);

        // 总食物排行榜需要额外附带“分数构成”，方便前端直接展开查看来源。
        Map<Integer, List<LeaderboardFoodSummaryDto>> totalFoodSummaryMap = buildTotalFoodSummaryMap(classId);

        return new ClassLeaderboardsDto(
                toLeaderboardItems(currentFoodRanking, StudentEntity::getFoodCount, Map.of()),
                toLeaderboardItems(totalFoodRanking, StudentEntity::getTotalFoodEarned, totalFoodSummaryMap),
                toLeaderboardItems(currentBadgesRanking, StudentEntity::getCurrentBadgesCount, Map.of()),
                toLeaderboardItems(totalBadgesRanking, StudentEntity::getTotalBadgesEarned, Map.of())
        );
    }

    private Map<Integer, List<LeaderboardFoodSummaryDto>> buildTotalFoodSummaryMap(Integer classId) {
        Map<Integer, List<LeaderboardFoodSummaryDto>> summaryMap = new LinkedHashMap<>();
        for (StudentTotalFoodSummaryEntity row : studentRankingStatsService.listTotalFoodSummaryByClass(classId)) {
            summaryMap.computeIfAbsent(row.getStudentId(), ignored -> new ArrayList<>()).add(
                    new LeaderboardFoodSummaryDto(
                            row.getRuleId(),
                            row.getRuleName(),
                            row.getAwardCount(),
                            row.getTotalFood()
                    )
            );
        }
        return summaryMap;
    }

    private List<LeaderboardStudentItemDto> toLeaderboardItems(
            List<StudentEntity> students,
            Function<StudentEntity, Integer> rankValueResolver,
            Map<Integer, List<LeaderboardFoodSummaryDto>> totalFoodSummaryMap
    ) {
        List<LeaderboardStudentItemDto> items = new ArrayList<>();
        for (StudentEntity student : students) {
            items.add(new LeaderboardStudentItemDto(
                    student.getId(),
                    student.getName(),
                    student.getPetType(),
                    student.getPetName(),
                    student.getGroupId(),
                    student.getGroup(),
                    defaultZero(student.getFoodCount()),
                    defaultZero(student.getTotalFoodEarned()),
                    defaultZero(student.getCurrentBadgesCount()),
                    defaultZero(student.getTotalBadgesEarned()),
                    defaultZero(rankValueResolver.apply(student)),
                    totalFoodSummaryMap.getOrDefault(student.getId(), List.of())
            ));
        }
        return items;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
