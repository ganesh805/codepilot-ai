package com.codepilot.repository;

import com.codepilot.entity.ChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, Long> {

    List<ChunkEmbedding> findByRepositoryId(Long repositoryId);

    long countByRepositoryId(Long repositoryId);

    @Query("SELECT COUNT(e) FROM ChunkEmbedding e WHERE e.repository.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    void deleteByRepositoryId(Long repositoryId);
}
