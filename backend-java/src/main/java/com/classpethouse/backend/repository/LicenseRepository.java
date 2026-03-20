package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<LicenseEntity, Integer> {

    /**
     * 原子更新激活码状态
     * @return 返回受影响的行数 (如果为 1 说明成功，0 说明激活码无效或已被抢占)
     */
    @Modifying
    @Query("UPDATE LicenseEntity l SET l.isUsed = true, l.usedBy = :userId, l.usedAt = :usedAt WHERE l.code = :code AND l.isUsed = false")
    int consumeLicense(
            @Param("code") String code,
            @Param("userId") Integer userId,
            @Param("usedAt") LocalDateTime usedAt
    );

    Optional<LicenseEntity> findByCodeAndIsUsedFalse(String code);

    List<LicenseEntity> findAllByOrderByCreatedAtDesc();

    long countByIsUsedTrue();
}
