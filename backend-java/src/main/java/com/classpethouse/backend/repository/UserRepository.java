package com.classpethouse.backend.repository;

import com.classpethouse.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameAndAdminTrue(String username);

    long countByIsActivatedTrue();

    long countByAdminTrue();
}
