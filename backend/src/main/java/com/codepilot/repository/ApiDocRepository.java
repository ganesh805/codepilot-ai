package com.codepilot.repository;

import com.codepilot.entity.ApiDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiDocRepository extends JpaRepository<ApiDoc, Long> {

    Optional<ApiDoc> findTopByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}
