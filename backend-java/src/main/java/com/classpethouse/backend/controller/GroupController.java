package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.GroupEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.GroupRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final AppSupportService appSupportService;

    public GroupController(GroupRepository groupRepository, StudentRepository studentRepository, AppSupportService appSupportService) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.appSupportService = appSupportService;
    }

    @GetMapping("/class/{classId}")
    public List<GroupEntity> listByClass(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());
        List<GroupEntity> groups = groupRepository.findByClassIdOrderBySortOrderAsc(classId);
        appSupportService.populateGroupStudents(groups);
        return groups;
    }

    @PostMapping
    public GroupEntity create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        String name = RequestUtils.string(body, "name");
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "分组名称不能为空");
        }
        long count = groupRepository.countByClassId(classId);
        if (count >= 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "最多创建50个分组");
        }

        GroupEntity group = new GroupEntity();
        group.setClassId(classId);
        group.setName(name.trim());
        group.setSortOrder((int) count);
        return groupRepository.save(group);
    }

    @PutMapping("/{id}")
    public GroupEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        GroupEntity group = appSupportService.requireGroup(id);
        appSupportService.requireOwnedClass(group.getClassId(), user.getId());

        if (body.containsKey("name")) {
            String name = RequestUtils.string(body, "name");
            if (name == null || name.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "分组名称不能为空");
            }
            group.setName(name.trim());
        }
        if (body.containsKey("sort_order")) {
            Integer sortOrder = RequestUtils.integer(body, "sort_order");
            if (sortOrder != null) {
                group.setSortOrder(sortOrder);
            }
        }
        return groupRepository.save(group);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        GroupEntity group = appSupportService.requireGroup(id);
        appSupportService.requireOwnedClass(group.getClassId(), user.getId());

        List<StudentEntity> students = studentRepository.findByGroupId(group.getId());
        students.forEach(student -> student.setGroupId(null));
        studentRepository.saveAll(students);
        groupRepository.delete(group);
        return Map.of("message", "删除成功");
    }

    @PostMapping("/random-assign")
    public Map<String, Object> randomAssign(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        List<GroupEntity> groups = groupRepository.findByClassIdOrderBySortOrderAsc(classId);
        if (groups.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先创建分组");
        }
        List<StudentEntity> students = studentRepository.findByClassId(classId);
        Collections.shuffle(students);
        for (int i = 0; i < students.size(); i++) {
            students.get(i).setGroupId(groups.get(i % groups.size()).getId());
        }
        studentRepository.saveAll(students);
        return Map.of("message", "随机分组完成");
    }
}
