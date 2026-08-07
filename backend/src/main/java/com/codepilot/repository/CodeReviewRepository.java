package com.codepilot.repository;

import com.codepilot.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
