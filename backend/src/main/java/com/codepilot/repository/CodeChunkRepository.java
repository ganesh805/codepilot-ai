package com.codepilot.repository;

import com.codepilot.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {

    List<CodeChunk> findByRepositoryIdOrderByFilePathAscChunkIndexAsc(Long repositoryId);

    long countByRepositoryId(Long repositoryId);

    @Query("SELECT COUNT(c) FROM CodeChunk c WHERE c.repository.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    void deleteByRepositoryId(Long repositoryId);

    @Query("SELECT c.language, COUNT(c) FROM CodeChunk c WHERE c.repository.id = :repositoryId GROUP BY c.language")
    List<Object[]> getLanguageStatsByRepositoryId(Long repositoryId);
}
