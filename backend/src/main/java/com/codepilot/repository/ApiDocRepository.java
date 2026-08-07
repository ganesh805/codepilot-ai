package com.codepilot.repository;

import com.codepilot.entity.ApiDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiDocRepository extends JpaRepository<ApiDoc, Long> {

    Optional<ApiDoc> findTopByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    @Query("SELECT COUNT(a) FROM ApiDoc a WHERE a.repository.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    void deleteByRepositoryId(Long repositoryId);
}
