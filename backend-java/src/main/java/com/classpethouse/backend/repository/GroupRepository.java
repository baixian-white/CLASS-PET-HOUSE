package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<GroupEntity, Integer> {
    List<GroupEntity> findByClassIdOrderBySortOrderAsc(Integer classId);

    long countByClassId(Integer classId);

    void deleteByClassId(Integer classId);
}
