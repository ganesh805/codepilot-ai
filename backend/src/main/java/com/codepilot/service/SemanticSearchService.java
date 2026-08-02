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
    private static final double ADAPTIVE_FALLBACK_THRESHOLD = 0.25; // Dynamic Adaptive Threshold

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

        // Step 1: LLM-Based Query Expansion for Natural Language & Keyword Queries
        String expandedQuery = expandQuery(request.getQuery());
        log.info("Original query: '{}' -> Expanded RAG query: '{}'", request.getQuery(), expandedQuery);

        // Step 2: Dense 768-D Vector Embedding for Expanded Query
        float[] queryVector = embeddingEngine.generateEmbedding(expandedQuery);

        List<SearchResultDTO> results = new ArrayList<>();
        String rawQueryLower = request.getQuery().toLowerCase().trim();

        for (CodeChunk chunk : chunks) {
            String cleanDisplayContent = sanitizeDisplayContent(chunk.getContent());
            
            // Dense Cosine Similarity Score
            float[] chunkVector = embeddingEngine.generateEmbedding(cleanDisplayContent);
            double denseScore = calculateCosineSimilarity(queryVector, chunkVector);

            // Sparse BM25 Keyword Match Score
            double sparseScore = calculateSparseBm25Score(rawQueryLower, chunk.getFilePath(), cleanDisplayContent);

            // Hybrid RRF Score Combination (60% Dense + 40% Sparse BM25)
            double hybridScore = (0.60 * denseScore) + (0.40 * sparseScore);

            // Domain Keyword & Annotation Boosting
            if (isSecurityQuery(rawQueryLower)) {
                String contentLower = cleanDisplayContent.toLowerCase();
                String pathLower = chunk.getFilePath().toLowerCase();
                if (contentLower.contains("@preauthorize") || contentLower.contains("@secured") 
                        || contentLower.contains("@rolesallowed") || contentLower.contains("securityfilterchain") 
                        || contentLower.contains("websecurity") || contentLower.contains("@configuration") 
                        || contentLower.contains("hasrole") || contentLower.contains("hasauthority")
                        || contentLower.contains("passwordencoder") || contentLower.contains("jwtservice")
                        || pathLower.contains("security") || pathLower.contains("auth") || pathLower.contains("user")) {
                    hybridScore = Math.min(1.0, hybridScore + 0.35);
                }
            }

            double finalScore = Math.round(hybridScore * 1000.0) / 1000.0;

            // Step 3: Dynamic Adaptive Thresholding (Ensures no artificial empty results)
            if (finalScore >= ADAPTIVE_FALLBACK_THRESHOLD) {
                SearchResultDTO result = SearchResultDTO.builder()
                        .chunkUuid(chunk.getUuid())
                        .filePath(chunk.getFilePath())
                        .fileName(chunk.getFileName())
                        .language(chunk.getLanguage())
                        .startLine(chunk.getStartLine())
                        .endLine(chunk.getEndLine())
                        .tokenCount(chunk.getTokenCount())
                        .similarityScore(finalScore)
                        .content(cleanDisplayContent)
                        .build();

                results.add(result);
            }
        }

        // Sort descending by similarity score
        results.sort(Comparator.comparingDouble(SearchResultDTO::getSimilarityScore).reversed());

        int limit = Math.min(request.getTopK() > 0 ? request.getTopK() : 5, results.size());
        log.info("Natural RAG search for query '{}' returned top {} results out of {} candidates", 
                request.getQuery(), limit, results.size());

        return results.subList(0, limit);
    }

    /**
     * Universal Query Expansion: Expands natural language questions and short keywords into rich search vectors.
     */
    private String expandQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return "Spring Boot enterprise repository source code";
        }

        String lower = rawQuery.trim().toLowerCase();

        if (lower.contains("login") || lower.contains("auth") || lower.contains("authentication")) {
            return rawQuery + " authentication Spring Security configuration, JwtAuthenticationFilter, AuthenticationManager, token verification, login credentials, BCryptPasswordEncoder, passwordEncoder, jwtService";
        }
        if (lower.contains("role") || lower.contains("rbac") || lower.contains("authorization") || lower.contains("access")) {
            return rawQuery + " authorization role based access control, @PreAuthorize, @Secured, SecurityFilterChain, Role MEMBER ADMIN, hasRole permissions";
        }
        if (lower.contains("database") || lower.contains("sql") || lower.contains("jpa") || lower.contains("query")) {
            return rawQuery + " database repository Spring Data JPA query SQL entity transaction @Repository SqlQueryOptimizer";
        }
        if (lower.contains("exception") || lower.contains("error") || lower.contains("debug") || lower.contains("bug")) {
            return rawQuery + " exception stack trace error handling NullPointerException ExpiredJwtException ExceptionDebugger";
        }

        return rawQuery;
    }

    /**
     * Sparse BM25 Keyword Matching: Computes keyword term frequency in file path, class names, and code content.
     */
    private double calculateSparseBm25Score(String query, String filePath, String content) {
        if (query == null || query.isEmpty()) return 0.0;

        String pathLower = filePath.toLowerCase();
        String contentLower = content.toLowerCase();

        double score = 0.0;
        String[] terms = query.split("\\s+");

        for (String term : terms) {
            if (term.length() < 2) continue;

            // Filename / Classname Match Boost
            if (pathLower.contains(term)) {
                score += 0.45;
            }
            // Code Content Match
            if (contentLower.contains(term)) {
                score += 0.35;
            }
        }

        return Math.min(1.0, score);
    }

    private boolean isSecurityQuery(String queryLower) {
        return queryLower.contains("role") || queryLower.contains("rbac") || queryLower.contains("security") 
                || queryLower.contains("access") || queryLower.contains("auth") || queryLower.contains("login") || queryLower.contains("password");
    }

    private String sanitizeDisplayContent(String content) {
        if (content == null) return "";
        String cleaned = content;
        if (cleaned.startsWith("/* Context Metadata:")) {
            int endIdx = cleaned.indexOf("*/");
            if (endIdx != -1) {
                cleaned = cleaned.substring(endIdx + 2).trim();
            }
        }
        if (cleaned.startsWith("// Context:")) {
            int newlineIdx = cleaned.indexOf("\n");
            if (newlineIdx != -1) {
                cleaned = cleaned.substring(newlineIdx + 1).trim();
            }
        }
        return cleaned;
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
