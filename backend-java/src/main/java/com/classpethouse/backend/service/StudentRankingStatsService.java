package com.classpethouse.backend.service;

import com.classpethouse.backend.entity.HistoryEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.StudentTotalFoodSummaryEntity;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.repository.StudentTotalFoodSummaryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 统一维护排行榜相关统计字段。
 * 这里把“当前食物分”和“总食物分构成”集中重建，避免多个控制器各自维护时出现统计漂移。
 */
@Service
public class StudentRankingStatsService {

    private final StudentRepository studentRepository;
    private final HistoryRepository historyRepository;
    private final StudentTotalFoodSummaryRepository studentTotalFoodSummaryRepository;

    public StudentRankingStatsService(
            StudentRepository studentRepository,
            HistoryRepository historyRepository,
            StudentTotalFoodSummaryRepository studentTotalFoodSummaryRepository
    ) {
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.studentTotalFoodSummaryRepository = studentTotalFoodSummaryRepository;
    }

    /**
     * 按历史积分记录重建学生的“当前食物分”和“总食物分”。
     * 当前食物分会受清零时间影响；总食物分只累计历史上获得过的正向积分。
     */
    public Map<Integer, StudentEntity> refreshFoodStats(Collection<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }

        // 同一批操作里可能会重复传入同一个学生 ID，这里先去重，避免重复删改同一批汇总数据。
        List<Integer> uniqueStudentIds = studentIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
        if (uniqueStudentIds.isEmpty()) {
            return Map.of();
        }

        List<StudentEntity> students = studentRepository.findAllById(uniqueStudentIds);
        if (students.isEmpty()) {
            return Map.of();
        }

        Map<Integer, StudentEntity> studentMap = new LinkedHashMap<>();
        for (StudentEntity student : students) {
            studentMap.put(student.getId(), student);
        }

        List<HistoryEntity> activeScoreHistory = historyRepository
                .findByStudentIdInAndTypeAndIsRevokedFalseOrderByStudentIdAscCreatedAtAscIdAsc(studentMap.keySet(), "score");

        Map<Integer, List<StudentTotalFoodSummaryEntity>> summaryMap = new HashMap<>();
        for (StudentEntity student : students) {
            summaryMap.put(student.getId(), new ArrayList<>());
            student.setFoodCount(0);
            student.setTotalFoodEarned(0);
        }

        Map<String, StudentTotalFoodSummaryEntity> summaryIndex = new HashMap<>();
        for (HistoryEntity record : activeScoreHistory) {
            StudentEntity student = studentMap.get(record.getStudentId());
            if (student == null) {
                continue;
            }

            Integer value = record.getValue() == null ? 0 : record.getValue();
            if (value > 0) {
                student.setTotalFoodEarned(student.getTotalFoodEarned() + value);

                // 总食物构成只统计“历史累计获得的正向食物分”。
                Integer ruleId = record.getRuleId();
                if (ruleId != null) {
                    String key = student.getId() + ":" + ruleId;
                    StudentTotalFoodSummaryEntity summary = summaryIndex.get(key);
                    if (summary == null) {
                        summary = new StudentTotalFoodSummaryEntity();
                        summary.setClassId(student.getClassId());
                        summary.setStudentId(student.getId());
                        summary.setRuleId(ruleId);
                        summary.setRuleName(record.getRuleName() == null ? "未命名规则" : record.getRuleName());
                        summaryIndex.put(key, summary);
                        summaryMap.get(student.getId()).add(summary);
                    }
                    summary.setAwardCount(summary.getAwardCount() + 1);
                    summary.setTotalFood(summary.getTotalFood() + value);
                }
            }

            // 当前食物分只回放“清零时间之后”的积分流水。
            if (shouldCountForCurrentFood(student, record)) {
                int currentFood = student.getFoodCount() == null ? 0 : student.getFoodCount();
                student.setFoodCount(Math.max(0, currentFood + value));
            }
        }

        studentRepository.saveAll(students);
        studentRepository.flush();

        // 先把这批学生的旧汇总直接批量删掉，再插入新汇总，避免旧行未落库删除时撞上唯一约束。
        studentTotalFoodSummaryRepository.deleteAllByStudentIdIn(uniqueStudentIds);
        studentTotalFoodSummaryRepository.flush();
        List<StudentTotalFoodSummaryEntity> summaries = summaryMap.values().stream()
                .flatMap(List::stream)
                .toList();
        if (!summaries.isEmpty()) {
            studentTotalFoodSummaryRepository.saveAll(summaries);
        }

        return studentMap;
    }

    /**
     * 当前进度清零时，只重置当前食物分，不影响历史累计获得的总食物分。
     */
    public void resetCurrentFoodProgress(Collection<Integer> studentIds, LocalDateTime resetAt) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        List<StudentEntity> students = studentRepository.findAllById(studentIds);
        if (students.isEmpty()) {
            return;
        }

        LocalDateTime appliedAt = resetAt == null ? LocalDateTime.now() : resetAt;
        for (StudentEntity student : students) {
            student.setFoodCount(0);
            student.setFoodResetAt(appliedAt);
        }
        studentRepository.saveAll(students);
    }

    /**
     * 规则改名后，汇总表中的展示名称同步更新，避免排行榜里出现旧名字。
     */
    public void renameFoodSummaryRuleName(Integer classId, Integer ruleId, String ruleName) {
        if (classId == null || ruleId == null || ruleName == null || ruleName.isBlank()) {
            return;
        }

        List<StudentTotalFoodSummaryEntity> rows = studentTotalFoodSummaryRepository.findByClassIdAndRuleId(classId, ruleId);
        if (rows.isEmpty()) {
            return;
        }
        for (StudentTotalFoodSummaryEntity row : rows) {
            row.setRuleName(ruleName);
        }
        studentTotalFoodSummaryRepository.saveAll(rows);
    }

    public List<StudentTotalFoodSummaryEntity> listTotalFoodSummary(Integer classId, Integer studentId) {
        return studentTotalFoodSummaryRepository
                .findByClassIdAndStudentIdOrderByTotalFoodDescAwardCountDescRuleNameAsc(classId, studentId);
    }

    /**
     * 按班级读取“总食物分构成”明细，供总食物排行榜一次性展示每位学生的分数组成。
     */
    public List<StudentTotalFoodSummaryEntity> listTotalFoodSummaryByClass(Integer classId) {
        return studentTotalFoodSummaryRepository
                .findByClassIdOrderByStudentIdAscTotalFoodDescAwardCountDescRuleNameAsc(classId);
    }

    /**
     * 同步勋章统计。
     * currentBadgesCount 表示当前可消费勋章数，totalBadgesEarned 只增不减，表示历史累计获得数。
     */
    public void syncBadgeStats(StudentEntity student, int previousBadgeCount) {
        int currentBadgeCount = student.getBadges() == null ? 0 : student.getBadges().size();
        student.setCurrentBadgesCount(currentBadgeCount);
        if (currentBadgeCount > previousBadgeCount) {
            int delta = currentBadgeCount - previousBadgeCount;
            student.setTotalBadgesEarned((student.getTotalBadgesEarned() == null ? 0 : student.getTotalBadgesEarned()) + delta);
        }
    }

    private boolean shouldCountForCurrentFood(StudentEntity student, HistoryEntity record) {
        LocalDateTime resetAt = student.getFoodResetAt();
        LocalDateTime createdAt = record.getCreatedAt();
        if (resetAt == null || createdAt == null) {
            return true;
        }
        return !createdAt.isBefore(resetAt);
    }
}
