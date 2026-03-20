package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.ExchangeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRecordRepository extends JpaRepository<ExchangeRecordEntity, Integer> {
    List<ExchangeRecordEntity> findTop200ByClassIdOrderByCreatedAtDesc(Integer classId);

    void deleteByClassId(Integer classId);

    void deleteByStudentId(Integer studentId);
}
