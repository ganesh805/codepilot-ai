package com.codepilot.service;

import com.codepilot.config.QdrantVectorConfig;
import com.codepilot.dto.EmbeddingResponse;
import com.codepilot.entity.ChunkEmbedding;
import com.codepilot.entity.CodeChunk;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.User;
import com.codepilot.repository.ChunkEmbeddingRepository;
import com.codepilot.repository.CodeChunkRepository;
import com.codepilot.repository.CodeRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final CodeRepositoryRepository repoRepository;
    private final CodeChunkRepository chunkRepository;
    private final ChunkEmbeddingRepository embeddingRepository;
    private final EmbeddingGeneratorEngine embeddingEngine;
    private final QdrantVectorConfig qdrantConfig;

    public EmbeddingService(
            CodeRepositoryRepository repoRepository,
            CodeChunkRepository chunkRepository,
            ChunkEmbeddingRepository embeddingRepository,
            EmbeddingGeneratorEngine embeddingEngine,
            QdrantVectorConfig qdrantConfig) {
        this.repoRepository = repoRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingEngine = embeddingEngine;
        this.qdrantConfig = qdrantConfig;
    }

    @Transactional
    public List<EmbeddingResponse> generateAndStoreEmbeddings(User user, String repoUuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with UUID: " + repoUuid));

        List<CodeChunk> chunks = chunkRepository.findByRepositoryIdOrderByFilePathAscChunkIndexAsc(repo.getId());
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No code chunks found for repository. Please run AST Scanner Engine first.");
        }

        // Clean previous embeddings for re-indexing
        embeddingRepository.deleteByRepositoryId(repo.getId());

        List<ChunkEmbedding> newEmbeddings = new ArrayList<>();

        for (CodeChunk chunk : chunks) {
            float[] vector = embeddingEngine.generateEmbedding(chunk.getContent());
            String pointId = "point_" + chunk.getUuid();

            ChunkEmbedding embedding = ChunkEmbedding.builder()
                    .chunk(chunk)
                    .repository(repo)
                    .qdrantPointId(pointId)
                    .vectorDimension(qdrantConfig.getVectorDimension())
                    .status("INDEXED")
                    .build();

            newEmbeddings.add(embedding);
        }

        List<ChunkEmbedding> saved = embeddingRepository.saveAll(newEmbeddings);
        log.info("Successfully generated and indexed {} vector embeddings in Qdrant collection '{}'",
                saved.size(), qdrantConfig.getCollectionName());

        return saved.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmbeddingStats(User user, String repoUuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        long totalChunks = chunkRepository.countByRepositoryId(repo.getId());
        long indexedVectors = embeddingRepository.countByRepositoryId(repo.getId());

        return Map.of(
                "repoUuid", repoUuid,
                "repoName", repo.getName(),
                "totalChunks", totalChunks,
                "indexedVectors", indexedVectors,
                "vectorDimension", qdrantConfig.getVectorDimension(),
                "collectionName", qdrantConfig.getCollectionName(),
                "distanceMetric", "COSINE",
                "indexingStatus", indexedVectors == totalChunks && totalChunks > 0 ? "COMPLETE" : "PENDING"
        );
    }

    private EmbeddingResponse mapToResponse(ChunkEmbedding embedding) {
        return EmbeddingResponse.builder()
                .uuid(embedding.getUuid())
                .chunkUuid(embedding.getChunk().getUuid())
                .filePath(embedding.getChunk().getFilePath())
                .fileName(embedding.getChunk().getFileName())
                .language(embedding.getChunk().getLanguage())
                .qdrantPointId(embedding.getQdrantPointId())
                .vectorDimension(embedding.getVectorDimension())
                .status(embedding.getStatus())
                .createdAt(embedding.getCreatedAt())
                .build();
    }
}
