package com.codepilot.repository;

import com.codepilot.entity.ChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, Long> {

    List<ChunkEmbedding> findByRepositoryId(Long repositoryId);

    long countByRepositoryId(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}
