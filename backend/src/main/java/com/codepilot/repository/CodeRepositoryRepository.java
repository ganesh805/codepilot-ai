package com.codepilot.repository;

import com.codepilot.entity.CodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {

    List<CodeRepository> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CodeRepository> findByUuidAndUserId(String uuid, Long userId);

    Optional<CodeRepository> findByUuid(String uuid);
}
