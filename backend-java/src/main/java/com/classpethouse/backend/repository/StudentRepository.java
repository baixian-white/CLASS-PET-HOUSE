package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Integer> {
    List<StudentEntity> findByClassIdOrderBySortOrderAscCreatedAtAsc(Integer classId);

    List<StudentEntity> findByClassIdOrderByFoodCountDescCreatedAtAscIdAsc(Integer classId);

    List<StudentEntity> findByClassIdOrderByTotalFoodEarnedDescCreatedAtAscIdAsc(Integer classId);

    List<StudentEntity> findByClassIdOrderByCurrentBadgesCountDescCreatedAtAscIdAsc(Integer classId);

    List<StudentEntity> findByClassIdOrderByTotalBadgesEarnedDescCreatedAtAscIdAsc(Integer classId);

    List<StudentEntity> findByClassId(Integer classId);

    List<StudentEntity> findByClassIdAndPetTypeIsNull(Integer classId);

    Optional<StudentEntity> findByClassIdAndName(Integer classId, String name);

    long countByClassId(Integer classId);

    List<StudentEntity> findByGroupId(Integer groupId);

    List<StudentEntity> findByIdIn(Collection<Integer> ids);

    Optional<StudentEntity> findByIdAndClassId(Integer id, Integer classId);

    void deleteByClassId(Integer classId);
}
