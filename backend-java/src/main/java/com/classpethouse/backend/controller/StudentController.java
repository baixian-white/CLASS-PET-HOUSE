package com.classpethouse.backend.controller;

import com.classpethouse.backend.config.AppLimits;
import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.StudentTotalFoodSummaryEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ExchangeRecordRepository;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.service.StudentRankingStatsService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final HistoryRepository historyRepository;
    private final ExchangeRecordRepository exchangeRecordRepository;
    private final AppSupportService appSupportService;
    private final StudentRankingStatsService studentRankingStatsService;

    public StudentController(
            StudentRepository studentRepository,
            HistoryRepository historyRepository,
            ExchangeRecordRepository exchangeRecordRepository,
            AppSupportService appSupportService,
            StudentRankingStatsService studentRankingStatsService
    ) {
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.exchangeRecordRepository = exchangeRecordRepository;
        this.appSupportService = appSupportService;
        this.studentRankingStatsService = studentRankingStatsService;
    }

    @GetMapping("/class/{classId}")
    public List<StudentEntity> listByClass(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());
        List<StudentEntity> students = studentRepository.findByClassIdOrderBySortOrderAscCreatedAtAsc(classId);
        appSupportService.populateGroups(students);
        return students;
    }

    @GetMapping("/{id}/total-food-summary")
    public List<StudentTotalFoodSummaryEntity> totalFoodSummary(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        StudentEntity student = appSupportService.requireStudent(id);
        appSupportService.requireOwnedClass(student.getClassId(), user.getId());
        return studentRankingStatsService.listTotalFoodSummary(student.getClassId(), student.getId());
    }

    @PostMapping
    public Object create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        Integer classId = RequestUtils.integer(body, "class_id");
        ClassEntity currentClass = appSupportService.requireOwnedClass(classId, user.getId());

        Object rawNames = body.get("names");
        if (rawNames instanceof List<?>) {
            List<String> names = RequestUtils.stringList(body, "names");
            if (names.size() > 200) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "单次最多添加200名学生");
            }
            List<StudentEntity> existingStudents = studentRepository.findByClassId(classId);
            Set<String> existingNames = new HashSet<>();
            existingStudents.forEach(student -> existingNames.add(student.getName()));

            List<String> validNames = new ArrayList<>();
            for (String rawName : names) {
                String name = safeTrim(rawName);
                if (isBlank(name) || name.length() > 50 || existingNames.contains(name)) {
                    continue;
                }
                validNames.add(name);
                existingNames.add(name);
            }

            ensureStudentCapacity(existingStudents.size(), validNames.size());

            List<StudentEntity> toCreate = new ArrayList<>();
            int nextSort = existingStudents.size();
            for (String name : validNames) {
                StudentEntity student = new StudentEntity();
                student.setClassId(currentClass.getId());
                student.setName(name);
                student.setSortOrder(nextSort++);
                toCreate.add(student);
            }
            List<StudentEntity> saved = studentRepository.saveAll(toCreate);
            return Map.of("created", saved.size(), "students", saved);
        }

        String name = safeTrim(RequestUtils.string(body, "name"));
        if (isBlank(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "学生姓名不能为空");
        }
        if (name.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "姓名最多50个字符");
        }
        ensureStudentCapacity(studentRepository.countByClassId(classId), 1);
        if (studentRepository.findByClassIdAndName(classId, name).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该班级已有同名学生");
        }

        StudentEntity student = new StudentEntity();
        student.setClassId(classId);
        student.setName(name);
        student.setSortOrder((int) studentRepository.countByClassId(classId));
        return studentRepository.save(student);
    }

    @PutMapping("/{id}")
    @Transactional
    public StudentEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        StudentEntity student = appSupportService.requireStudent(id);
        appSupportService.requireOwnedClass(student.getClassId(), user.getId());
        int previousBadgeCount = student.getCurrentBadgesCount() == null
                ? (student.getBadges() == null ? 0 : student.getBadges().size())
                : student.getCurrentBadgesCount();

        if (body.containsKey("name")) {
            String name = safeTrim(RequestUtils.string(body, "name"));
            if (isBlank(name)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "学生姓名不能为空");
            }
            student.setName(name);
        }
        if (body.containsKey("pet_type")) {
            student.setPetType(RequestUtils.string(body, "pet_type"));
        }
        if (body.containsKey("pet_name")) {
            student.setPetName(RequestUtils.string(body, "pet_name"));
        }
        if (body.containsKey("badges")) {
            student.setBadges(new ArrayList<>(RequestUtils.mapList(body, "badges")));
            // 勋章明细变化后，立即把当前/累计勋章统计同步到排行榜字段。
            studentRankingStatsService.syncBadgeStats(student, previousBadgeCount);
        }
        if (body.containsKey("sort_order")) {
            Integer sortOrder = RequestUtils.integer(body, "sort_order");
            if (sortOrder != null) {
                student.setSortOrder(sortOrder);
            }
        }
        if (body.containsKey("group_id")) {
            student.setGroupId(RequestUtils.integer(body, "group_id"));
        }
        if (body.containsKey("food_count")) {
            Integer foodCount = RequestUtils.integer(body, "food_count");
            if (foodCount != null && foodCount == 0) {
                student.setFoodCount(0);
                // 食物分被手动清零后，从这个时间点开始计算“当前宠物食物排行榜”。
                student.setFoodResetAt(LocalDateTime.now());
            }
        }

        return studentRepository.save(student);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);

        StudentEntity student = appSupportService.requireStudent(id);
        appSupportService.requireOwnedClass(student.getClassId(), user.getId());
        historyRepository.deleteByStudentId(student.getId());
        exchangeRecordRepository.deleteByStudentId(student.getId());
        studentRepository.delete(student);
        return Map.of("message", "删除成功");
    }

    @PostMapping("/reset-all")
    @Transactional
    public Map<String, Object> resetAll(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        List<StudentEntity> students = studentRepository.findByClassId(classId);
        LocalDateTime resetAt = LocalDateTime.now();
        for (StudentEntity student : students) {
            student.setFoodCount(0);
            student.setFoodResetAt(resetAt);
            student.setPetType(null);
            student.setPetName(null);
        }
        studentRepository.saveAll(students);
        return Map.of("message", "全班进度已重置");
    }

    @PostMapping("/random-pets")
    public Map<String, Object> randomPets(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        Object rawPets = body.get("pets");
        if (!(rawPets instanceof List<?> pets) || pets.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "宠物列表不能为空");
        }

        List<StudentEntity> students = studentRepository.findByClassIdAndPetTypeIsNull(classId);
        if (students.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "没有需要分配宠物的学生");
        }

        int count = 0;
        for (StudentEntity student : students) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pet = (Map<String, Object>) pets.get((int) (Math.random() * pets.size()));
            student.setPetType(RequestUtils.string(pet, "id"));
            student.setPetName(RequestUtils.string(pet, "name"));
            count++;
        }
        studentRepository.saveAll(students);
        return Map.of("message", "已为" + count + "名学生随机分配宠物");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private void ensureStudentCapacity(long existingCount, int addingCount) {
        long remaining = AppLimits.MAX_STUDENTS_PER_CLASS - existingCount;
        if (addingCount <= remaining) {
            return;
        }
        if (remaining <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "一个班级最多" + AppLimits.MAX_STUDENTS_PER_CLASS + "位学生");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "当前班级最多还能添加" + remaining + "位学生");
    }
}
