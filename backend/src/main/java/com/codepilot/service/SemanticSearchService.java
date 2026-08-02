package com.codepilot.service;

import com.codepilot.dto.SearchRequest;
import com.codepilot.dto.SearchResultDTO;
import com.codepilot.entity.CodeChunk;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeChunkRepository;
import com.codepilot.repository.CodeRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final CodeRepositoryRepository repoRepository;
    private final CodeChunkRepository chunkRepository;
    private final EmbeddingGeneratorEngine embeddingEngine;

    public SemanticSearchService(
            CodeRepositoryRepository repoRepository,
            CodeChunkRepository chunkRepository,
            EmbeddingGeneratorEngine embeddingEngine) {
        this.repoRepository = repoRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingEngine = embeddingEngine;
    }

    @Transactional(readOnly = true)
    public List<SearchResultDTO> searchCodebase(User user, String repoUuid, SearchRequest request) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with UUID: " + repoUuid));

        List<CodeChunk> chunks = chunkRepository.findByRepositoryIdOrderByFilePathAscChunkIndexAsc(repo.getId());
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert query string into 768-D normalized vector using text-embedding-004
        float[] queryVector = embeddingEngine.generateEmbedding(request.getQuery());

        List<SearchResultDTO> results = new ArrayList<>();
        String queryLower = request.getQuery().toLowerCase();
        boolean isSecurityRbacQuery = queryLower.contains("role") || queryLower.contains("rbac") || queryLower.contains("security") || queryLower.contains("access control") || queryLower.contains("auth") || queryLower.contains("permission");

        for (CodeChunk chunk : chunks) {
            float[] chunkVector = embeddingEngine.generateEmbedding(chunk.getContent());
            double score = calculateCosineSimilarity(queryVector, chunkVector);
            String contentLower = chunk.getContent().toLowerCase();
            String pathLower = chunk.getFilePath().toLowerCase();

            // 1. Exact string keyword match boost
            if (contentLower.contains(queryLower) || pathLower.contains(queryLower)) {
                score = Math.min(1.0, score + 0.25);
            }

            // 2. Annotation Weight Boosting for Security & Access Control (@PreAuthorize, @Secured, @RolesAllowed, @Configuration)
            if (isSecurityRbacQuery) {
                if (contentLower.contains("@preauthorize") || contentLower.contains("@secured") 
                        || contentLower.contains("@rolesallowed") || contentLower.contains("securityfilterchain") 
                        || contentLower.contains("websecurityconfigureradapter") || contentLower.contains("@configuration") 
                        || contentLower.contains("hasrole") || contentLower.contains("hasauthority") || contentLower.contains("haspermission")
                        || pathLower.contains("security") || pathLower.contains("auth")) {
                    score = Math.min(1.0, score + 0.40);
                }
            }

            SearchResultDTO result = SearchResultDTO.builder()
                    .chunkUuid(chunk.getUuid())
                    .filePath(chunk.getFilePath())
                    .fileName(chunk.getFileName())
                    .language(chunk.getLanguage())
                    .startLine(chunk.getStartLine())
                    .endLine(chunk.getEndLine())
                    .tokenCount(chunk.getTokenCount())
                    .similarityScore(Math.round(score * 1000.0) / 1000.0)
                    .content(chunk.getContent())
                    .build();

            results.add(result);
        }

        // Sort descending by similarity score
        results.sort(Comparator.comparingDouble(SearchResultDTO::getSimilarityScore).reversed());

        int limit = Math.min(request.getTopK() > 0 ? request.getTopK() : 5, results.size());
        log.info("Semantic search for query '{}' returned top {} results out of {} chunks", request.getQuery(), limit, chunks.size());

        return results.subList(0, limit);
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
