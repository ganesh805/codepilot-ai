package com.codepilot.repository;

import com.codepilot.entity.SqlOptimization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SqlOptimizationRepository extends JpaRepository<SqlOptimization, Long> {

    List<SqlOptimization> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
