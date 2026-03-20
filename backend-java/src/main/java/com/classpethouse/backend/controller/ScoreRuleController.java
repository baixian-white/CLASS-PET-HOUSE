package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.ScoreRuleEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ScoreRuleRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.service.StudentRankingStatsService;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/score-rules")
public class ScoreRuleController {

    private final ScoreRuleRepository scoreRuleRepository;
    private final AppSupportService appSupportService;
    private final StudentRankingStatsService studentRankingStatsService;

    public ScoreRuleController(
            ScoreRuleRepository scoreRuleRepository,
            AppSupportService appSupportService,
            StudentRankingStatsService studentRankingStatsService
    ) {
        this.scoreRuleRepository = scoreRuleRepository;
        this.appSupportService = appSupportService;
        this.studentRankingStatsService = studentRankingStatsService;
    }

    @GetMapping("/class/{classId}")
    public List<ScoreRuleEntity> list(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());
        return scoreRuleRepository.findByClassIdOrderBySortOrderAsc(classId);
    }

    @PostMapping
    public ScoreRuleEntity create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        String name = RequestUtils.string(body, "name");
        Integer value = RequestUtils.integer(body, "value");
        if (name == null || name.isBlank() || value == null || value == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "名称和分值不能为空，分值不能为0");
        }
        long count = scoreRuleRepository.countByClassId(classId);
        if (count >= 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "最多创建50条规则");
        }

        ScoreRuleEntity rule = new ScoreRuleEntity();
        rule.setClassId(classId);
        rule.setName(name.trim());
        rule.setIcon(RequestUtils.string(body, "icon") == null ? "⭐" : RequestUtils.string(body, "icon"));
        rule.setValue(value);
        rule.setSortOrder((int) count);
        return scoreRuleRepository.save(rule);
    }

    @PutMapping("/{id}")
    public ScoreRuleEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        ScoreRuleEntity rule = appSupportService.requireRule(id);
        appSupportService.requireOwnedClass(rule.getClassId(), user.getId());
        boolean ruleNameChanged = false;

        if (body.containsKey("name")) {
            String name = RequestUtils.string(body, "name");
            if (name == null || name.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "规则名称不能为空");
            }
            rule.setName(name.trim());
            ruleNameChanged = true;
        }
        if (body.containsKey("icon")) {
            rule.setIcon(RequestUtils.string(body, "icon"));
        }
        if (body.containsKey("value")) {
            Integer value = RequestUtils.integer(body, "value");
            if (value != null) {
                rule.setValue(value);
            }
        }
        if (body.containsKey("sort_order")) {
            Integer sortOrder = RequestUtils.integer(body, "sort_order");
            if (sortOrder != null) {
                rule.setSortOrder(sortOrder);
            }
        }
        ScoreRuleEntity saved = scoreRuleRepository.save(rule);
        if (ruleNameChanged) {
            studentRankingStatsService.renameFoodSummaryRuleName(saved.getClassId(), saved.getId(), saved.getName());
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        ScoreRuleEntity rule = appSupportService.requireRule(id);
        appSupportService.requireOwnedClass(rule.getClassId(), user.getId());
        scoreRuleRepository.delete(rule);
        return Map.of("message", "删除成功");
    }
}
