package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.StudentTotalFoodSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudentTotalFoodSummaryRepository extends JpaRepository<StudentTotalFoodSummaryEntity, Integer> {
    List<StudentTotalFoodSummaryEntity> findByClassIdAndStudentIdOrderByTotalFoodDescAwardCountDescRuleNameAsc(Integer classId, Integer studentId);

    List<StudentTotalFoodSummaryEntity> findByClassIdOrderByStudentIdAscTotalFoodDescAwardCountDescRuleNameAsc(Integer classId);

    List<StudentTotalFoodSummaryEntity> findByStudentIdIn(Collection<Integer> studentIds);

    void deleteByStudentId(Integer studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StudentTotalFoodSummaryEntity s where s.studentId in :studentIds")
    int deleteAllByStudentIdIn(@Param("studentIds") Collection<Integer> studentIds);

    List<StudentTotalFoodSummaryEntity> findByClassIdAndRuleId(Integer classId, Integer ruleId);
}
