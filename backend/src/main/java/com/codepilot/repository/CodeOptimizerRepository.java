package com.codepilot.repository;

import com.codepilot.entity.CodeOptimizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeOptimizerRepository extends JpaRepository<CodeOptimizer, Long> {

    List<CodeOptimizer> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
