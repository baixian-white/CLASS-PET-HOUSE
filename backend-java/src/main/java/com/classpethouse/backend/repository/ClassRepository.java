package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Integer> {
    List<ClassEntity> findByUserIdOrderBySortOrderAscCreatedAtAsc(Integer userId);

    Optional<ClassEntity> findByIdAndUserId(Integer id, Integer userId);

    long countByUserId(Integer userId);
}
