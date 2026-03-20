package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.ShopItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItemEntity, Integer> {
    List<ShopItemEntity> findByClassIdOrderByCreatedAtAsc(Integer classId);

    long countByClassId(Integer classId);

    void deleteByClassId(Integer classId);
}
