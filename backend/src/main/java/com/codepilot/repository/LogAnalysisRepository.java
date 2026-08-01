package com.codepilot.repository;

import com.codepilot.entity.LogAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    List<LogAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
