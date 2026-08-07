package com.codepilot.repository;

import com.codepilot.entity.ExceptionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionAnalysisRepository extends JpaRepository<ExceptionAnalysis, Long> {

    List<ExceptionAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
