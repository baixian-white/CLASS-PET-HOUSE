package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.HistoryEntity;
import com.classpethouse.backend.entity.ScoreRuleEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.service.StudentRankingStatsService;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryRepository historyRepository;
    private final StudentRepository studentRepository;
    private final AppSupportService appSupportService;
    private final StudentRankingStatsService studentRankingStatsService;

    public HistoryController(
            HistoryRepository historyRepository,
            StudentRepository studentRepository,
            AppSupportService appSupportService,
            StudentRankingStatsService studentRankingStatsService
    ) {
        this.historyRepository = historyRepository;
        this.studentRepository = studentRepository;
        this.appSupportService = appSupportService;
        this.studentRankingStatsService = studentRankingStatsService;
    }

    @GetMapping("/class/{classId}")
    public Map<String, Object> list(
            @PathVariable Integer classId,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(name = "student_id", required = false) Integer studentId,
            HttpServletRequest request
    ) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());

        int safeLimit = Math.max(1, Math.min(limit == null ? 50 : limit, 200));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int page = safeOffset / safeLimit;

        Page<HistoryEntity> result = studentId == null
                ? historyRepository.findByClassIdOrderByCreatedAtDesc(classId, PageRequest.of(page, safeLimit))
                : historyRepository.findByClassIdAndStudentIdOrderByCreatedAtDesc(classId, studentId, PageRequest.of(page, safeLimit));

        List<HistoryEntity> rows = result.getContent();
        appSupportService.populateStudentRefs(rows);
        return Map.of(
                "count", result.getTotalElements(),
                "rows", rows
        );
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        Integer classId = RequestUtils.integer(body, "class_id");
        ClassEntity currentClass = appSupportService.requireOwnedClass(classId, user.getId());
        Integer ruleId = RequestUtils.integer(body, "rule_id");
        String recordType = RequestUtils.string(body, "type");
        List<Integer> studentIds = RequestUtils.integerList(body, "student_ids");
        if (studentIds.isEmpty()) {
            Integer singleStudentId = RequestUtils.integer(body, "student_ids");
            if (singleStudentId != null) {
                studentIds = List.of(singleStudentId);
            }
        }
        if (studentIds.isEmpty()) {
            Integer singleStudentId = RequestUtils.integer(body, "student_id");
            if (singleStudentId != null) {
                studentIds = List.of(singleStudentId);
            }
        }
        if (studentIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "学生不存在");
        }
        if (studentIds.size() > 200) {
            studentIds = studentIds.subList(0, 200);
        }

        List<Map<String, Object>> results = new ArrayList<>();

        if (ruleId == null && "graduate".equals(recordType)) {
            for (Integer studentId : studentIds) {
                StudentEntity student = studentRepository.findByIdAndClassId(studentId, currentClass.getId()).orElse(null);
                if (student == null) {
                    continue;
                }
                HistoryEntity record = new HistoryEntity();
                record.setClassId(currentClass.getId());
                record.setStudentId(studentId);
                record.setRuleId(null);
                record.setRuleName("宠物毕业");
                record.setValue(0);
                record.setType("graduate");
                historyRepository.save(record);
                results.add(Map.of(
                        "student_id", studentId,
                        "record_id", record.getId()
                ));
            }
            return Map.of("results", results);
        }

        ScoreRuleEntity rule = appSupportService.requireRule(ruleId);
        if (!rule.getClassId().equals(currentClass.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "规则不属于当前班级");
        }

        List<Integer> refreshedStudentIds = new ArrayList<>();
        for (Integer studentId : studentIds) {
            StudentEntity student = studentRepository.findByIdAndClassId(studentId, currentClass.getId()).orElse(null);
            if (student == null) {
                continue;
            }

            HistoryEntity record = new HistoryEntity();
            record.setClassId(currentClass.getId());
            record.setStudentId(studentId);
            record.setRuleId(rule.getId());
            record.setRuleName(rule.getName());
            record.setValue(rule.getValue());
            record.setType("score");
            historyRepository.save(record);
            refreshedStudentIds.add(studentId);
        }

        Map<Integer, StudentEntity> refreshedStudents = studentRankingStatsService.refreshFoodStats(refreshedStudentIds);

        for (Integer studentId : refreshedStudentIds) {
            StudentEntity student = refreshedStudents.get(studentId);
            if (student == null) {
                continue;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("student_id", studentId);
            result.put("new_food", student.getFoodCount());
            results.add(result);
        }

        return Map.of("results", results);
    }

    @PostMapping("/revoke")
    @Transactional
    public Map<String, Object> revoke(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer recordId = RequestUtils.integer(body, "record_id");

        HistoryEntity record = historyRepository.findById(recordId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "记录不存在或已撤回"));
        if (Boolean.TRUE.equals(record.getIsRevoked())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "记录不存在或已撤回");
        }
        appSupportService.requireOwnedClass(record.getClassId(), user.getId());

        record.setIsRevoked(true);
        historyRepository.save(record);
        studentRankingStatsService.refreshFoodStats(List.of(record.getStudentId()));
        return Map.of("message", "撤回成功");
    }

    @PostMapping("/revoke-batch")
    @Transactional
    public Map<String, Object> revokeBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        List<Integer> recordIds = RequestUtils.integerList(body, "record_ids");
        if (recordIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数错误");
        }
        if (recordIds.size() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "单次最多撤回100条");
        }

        int count = 0;
        List<Integer> refreshedStudentIds = new ArrayList<>();
        for (Integer recordId : recordIds) {
            HistoryEntity record = historyRepository.findById(recordId).orElse(null);
            if (record == null || Boolean.TRUE.equals(record.getIsRevoked())) {
                continue;
            }
            appSupportService.requireOwnedClass(record.getClassId(), user.getId());
            record.setIsRevoked(true);
            historyRepository.save(record);
            refreshedStudentIds.add(record.getStudentId());
            count++;
        }
        studentRankingStatsService.refreshFoodStats(refreshedStudentIds);
        return Map.of("message", "已撤回" + count + "条记录");
    }

    @PostMapping("/batch-delete")
    @Transactional
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        List<Integer> recordIds = RequestUtils.integerList(body, "record_ids");
        if (recordIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数错误");
        }
        if (recordIds.size() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "单次最多删除200条");
        }

        List<HistoryEntity> records = historyRepository.findByIdIn(recordIds);
        List<Integer> refreshedStudentIds = new ArrayList<>();
        for (HistoryEntity record : records) {
            appSupportService.requireOwnedClass(record.getClassId(), user.getId());
            refreshedStudentIds.add(record.getStudentId());
        }
        historyRepository.deleteAll(records);
        studentRankingStatsService.refreshFoodStats(refreshedStudentIds);
        return Map.of("message", "删除成功");
    }
}
