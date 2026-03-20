package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.ScoreRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRuleRepository extends JpaRepository<ScoreRuleEntity, Integer> {
    List<ScoreRuleEntity> findByClassIdOrderBySortOrderAsc(Integer classId);

    long countByClassId(Integer classId);

    void deleteByClassId(Integer classId);
}
