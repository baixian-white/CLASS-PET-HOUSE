package com.classpethouse.backend.service;

import com.classpethouse.backend.dto.IdNameDto;
import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.ExchangeRecordEntity;
import com.classpethouse.backend.entity.GroupEntity;
import com.classpethouse.backend.entity.HistoryEntity;
import com.classpethouse.backend.entity.ScoreRuleEntity;
import com.classpethouse.backend.entity.ShopItemEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ClassRepository;
import com.classpethouse.backend.repository.GroupRepository;
import com.classpethouse.backend.repository.ScoreRuleRepository;
import com.classpethouse.backend.repository.ShopItemRepository;
import com.classpethouse.backend.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppSupportService {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final ScoreRuleRepository scoreRuleRepository;
    private final ShopItemRepository shopItemRepository;

    public AppSupportService(
            ClassRepository classRepository,
            StudentRepository studentRepository,
            GroupRepository groupRepository,
            ScoreRuleRepository scoreRuleRepository,
            ShopItemRepository shopItemRepository
    ) {
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.scoreRuleRepository = scoreRuleRepository;
        this.shopItemRepository = shopItemRepository;
    }

    public ClassEntity requireOwnedClass(Integer classId, Integer userId) {
        return classRepository.findByIdAndUserId(classId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "班级不存在"));
    }

    public StudentEntity requireStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "学生不存在"));
    }

    public GroupEntity requireGroup(Integer groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "分组不存在"));
    }

    public ScoreRuleEntity requireRule(Integer ruleId) {
        return scoreRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "规则不存在"));
    }

    public ShopItemEntity requireShopItem(Integer itemId) {
        return shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    public void populateGroups(List<StudentEntity> students) {
        List<Integer> groupIds = students.stream()
                .map(StudentEntity::getGroupId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return;
        }
        Map<Integer, IdNameDto> groupMap = groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(GroupEntity::getId, g -> new IdNameDto(g.getId(), g.getName())));
        students.forEach(student -> student.setGroup(groupMap.get(student.getGroupId())));
    }

    public void populateGroupStudents(List<GroupEntity> groups) {
        if (groups.isEmpty()) {
            return;
        }
        List<Integer> classIds = groups.stream().map(GroupEntity::getClassId).distinct().toList();
        Map<Integer, List<StudentEntity>> groupedStudents = new HashMap<>();
        for (Integer classId : classIds) {
            for (StudentEntity student : studentRepository.findByClassIdOrderBySortOrderAscCreatedAtAsc(classId)) {
                if (student.getGroupId() != null) {
                    groupedStudents.computeIfAbsent(student.getGroupId(), ignored -> new java.util.ArrayList<>()).add(student);
                }
            }
        }
        groups.forEach(group -> group.setStudents(groupedStudents.getOrDefault(group.getId(), List.of())));
    }

    public void populateStudentRefs(Collection<HistoryEntity> rows) {
        List<Integer> studentIds = rows.stream().map(HistoryEntity::getStudentId).distinct().toList();
        Map<Integer, IdNameDto> studentMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(StudentEntity::getId, s -> new IdNameDto(s.getId(), s.getName())));
        rows.forEach(row -> row.setStudent(studentMap.get(row.getStudentId())));
    }

    public void populateStudentRefsForExchange(Collection<ExchangeRecordEntity> rows) {
        List<Integer> studentIds = rows.stream().map(ExchangeRecordEntity::getStudentId).distinct().toList();
        Map<Integer, IdNameDto> studentMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(StudentEntity::getId, s -> new IdNameDto(s.getId(), s.getName())));
        rows.forEach(row -> row.setStudent(studentMap.get(row.getStudentId())));
    }
}
