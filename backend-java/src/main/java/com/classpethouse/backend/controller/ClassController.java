package com.classpethouse.backend.controller;

import com.classpethouse.backend.config.AppLimits;
import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.ScoreRuleEntity;
import com.classpethouse.backend.entity.ShopItemEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ClassRepository;
import com.classpethouse.backend.repository.ExchangeRecordRepository;
import com.classpethouse.backend.repository.GroupRepository;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.ScoreRuleRepository;
import com.classpethouse.backend.repository.ShopItemRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final ScoreRuleRepository scoreRuleRepository;
    private final ShopItemRepository shopItemRepository;
    private final GroupRepository groupRepository;
    private final HistoryRepository historyRepository;
    private final ExchangeRecordRepository exchangeRecordRepository;

    public ClassController(
            ClassRepository classRepository,
            StudentRepository studentRepository,
            ScoreRuleRepository scoreRuleRepository,
            ShopItemRepository shopItemRepository,
            GroupRepository groupRepository,
            HistoryRepository historyRepository,
            ExchangeRecordRepository exchangeRecordRepository
    ) {
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.scoreRuleRepository = scoreRuleRepository;
        this.shopItemRepository = shopItemRepository;
        this.groupRepository = groupRepository;
        this.historyRepository = historyRepository;
        this.exchangeRecordRepository = exchangeRecordRepository;
    }

    @GetMapping
    public List<ClassEntity> list(HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        return classRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(user.getId());
    }

    @PostMapping
    public ClassEntity create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        String name = safeTrim(RequestUtils.string(body, "name"));
        if (isBlank(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "班级名称不能为空");
        }
        long count = classRepository.countByUserId(user.getId());
        if (count >= AppLimits.MAX_CLASSES_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "一个账号最多创建" + AppLimits.MAX_CLASSES_PER_USER + "个班级");
        }

        ClassEntity entity = new ClassEntity();
        entity.setUserId(user.getId());
        entity.setName(name);
        entity.setSortOrder((int) count);
        return classRepository.save(entity);
    }

    @PutMapping("/{id}")
    public ClassEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        ClassEntity entity = classRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "班级不存在"));

        if (body.containsKey("name")) {
            String name = safeTrim(RequestUtils.string(body, "name"));
            if (isBlank(name)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "班级名称不能为空");
            }
            entity.setName(name);
        }
        if (body.containsKey("system_name")) {
            entity.setSystemName(RequestUtils.string(body, "system_name"));
        }
        if (body.containsKey("theme")) {
            entity.setTheme(RequestUtils.string(body, "theme"));
        }
        if (body.containsKey("sort_order")) {
            Integer sortOrder = RequestUtils.integer(body, "sort_order");
            if (sortOrder != null) {
                entity.setSortOrder(sortOrder);
            }
        }
        if (body.containsKey("growth_stages")) {
            Object raw = body.get("growth_stages");
            if (!(raw instanceof List<?> rawList)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "成长阶段格式不正确");
            }
            List<Integer> stages = new ArrayList<>();
            for (Object item : rawList) {
                Integer value = RequestUtils.integer(item);
                if (value == null || value < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "成长阶段格式不正确");
                }
                stages.add(value);
            }
            if (stages.size() < 2 || stages.size() > 20) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "成长阶段格式不正确");
            }
            entity.setGrowthStages(stages);
        }

        return classRepository.save(entity);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        if (classRepository.countByUserId(user.getId()) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "至少保留一个班级");
        }
        ClassEntity entity = classRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "班级不存在"));

        historyRepository.deleteByClassId(entity.getId());
        exchangeRecordRepository.deleteByClassId(entity.getId());
        studentRepository.deleteByClassId(entity.getId());
        scoreRuleRepository.deleteByClassId(entity.getId());
        shopItemRepository.deleteByClassId(entity.getId());
        groupRepository.deleteByClassId(entity.getId());
        classRepository.delete(entity);
        return Map.of("message", "删除成功");
    }

    @PostMapping("/copy-config")
    @Transactional
    public Map<String, Object> copyConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        Integer fromClassId = RequestUtils.integer(body, "from_class_id");
        Integer toClassId = RequestUtils.integer(body, "to_class_id");
        if (fromClassId == null || toClassId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "班级不存在");
        }
        ClassEntity fromClass = classRepository.findByIdAndUserId(fromClassId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "班级不存在"));
        ClassEntity toClass = classRepository.findByIdAndUserId(toClassId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "班级不存在"));
        if (fromClass.getId().equals(toClass.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能复制到自身");
        }

        toClass.setGrowthStages(fromClass.getGrowthStages());
        classRepository.save(toClass);

        List<ScoreRuleEntity> copiedRules = scoreRuleRepository.findByClassIdOrderBySortOrderAsc(fromClassId).stream().map(rule -> {
            ScoreRuleEntity entity = new ScoreRuleEntity();
            entity.setClassId(toClassId);
            entity.setName(rule.getName());
            entity.setIcon(rule.getIcon());
            entity.setValue(rule.getValue());
            entity.setSortOrder(rule.getSortOrder());
            return entity;
        }).toList();
        scoreRuleRepository.saveAll(copiedRules);

        List<ShopItemEntity> copiedItems = shopItemRepository.findByClassIdOrderByCreatedAtAsc(fromClassId).stream().map(item -> {
            ShopItemEntity entity = new ShopItemEntity();
            entity.setClassId(toClassId);
            entity.setName(item.getName());
            entity.setDescription(item.getDescription());
            entity.setIcon(item.getIcon());
            entity.setPrice(item.getPrice());
            entity.setStock(item.getStock());
            return entity;
        }).toList();
        shopItemRepository.saveAll(copiedItems);

        return Map.of("message", "配置复制成功");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
