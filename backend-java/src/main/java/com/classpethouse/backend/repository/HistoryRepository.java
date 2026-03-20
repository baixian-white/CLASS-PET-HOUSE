package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.HistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HistoryRepository extends JpaRepository<HistoryEntity, Integer> {
    Page<HistoryEntity> findByClassIdOrderByCreatedAtDesc(Integer classId, Pageable pageable);

    Page<HistoryEntity> findByClassIdAndStudentIdOrderByCreatedAtDesc(Integer classId, Integer studentId, Pageable pageable);

    List<HistoryEntity> findByStudentIdInAndTypeAndIsRevokedFalseOrderByStudentIdAscCreatedAtAscIdAsc(Collection<Integer> studentIds, String type);

    List<HistoryEntity> findByIdIn(Collection<Integer> ids);

    void deleteByClassId(Integer classId);

    void deleteByStudentId(Integer studentId);
}
