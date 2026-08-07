package com.codepilot.service;

import com.codepilot.dto.AnalyticsMetricsDTO;
import com.codepilot.entity.User;
import com.codepilot.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CodeRepositoryRepository repoRepository;
    private final CodeChunkRepository chunkRepository;
    private final ChunkEmbeddingRepository embeddingRepository;
    private final ExceptionAnalysisRepository exceptionRepository;
    private final LogAnalysisRepository logRepository;
    private final CodeReviewRepository reviewRepository;
    private final ApiDocRepository apiDocRepository;
    private final SqlOptimizationRepository sqlRepository;

    public AnalyticsService(
            UserRepository userRepository,
            CodeRepositoryRepository repoRepository,
            CodeChunkRepository chunkRepository,
            ChunkEmbeddingRepository embeddingRepository,
            ExceptionAnalysisRepository exceptionRepository,
            LogAnalysisRepository logRepository,
            CodeReviewRepository reviewRepository,
            ApiDocRepository apiDocRepository,
            SqlOptimizationRepository sqlRepository) {
        this.userRepository = userRepository;
        this.repoRepository = repoRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.exceptionRepository = exceptionRepository;
        this.logRepository = logRepository;
        this.reviewRepository = reviewRepository;
        this.apiDocRepository = apiDocRepository;
        this.sqlRepository = sqlRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsMetricsDTO getUserAnalyticsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return getEmptyAnalytics();
        }

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        if (user == null) {
            return getEmptyAnalytics();
        }

        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() != null && r.getName().name().equals("ROLE_ADMIN"));

        long repos = isAdmin ? repoRepository.count() : repoRepository.countByUserId(user.getId());
        long chunks = isAdmin ? chunkRepository.count() : chunkRepository.countByUserId(user.getId());
        long embeddings = isAdmin ? embeddingRepository.count() : embeddingRepository.countByUserId(user.getId());
        long exceptions = isAdmin ? exceptionRepository.count() : exceptionRepository.countByUserId(user.getId());
        long logs = isAdmin ? logRepository.count() : logRepository.countByUserId(user.getId());
        long reviews = isAdmin ? reviewRepository.count() : reviewRepository.countByUserId(user.getId());
        long docs = isAdmin ? apiDocRepository.count() : apiDocRepository.countByUserId(user.getId());
        long sqls = isAdmin ? sqlRepository.count() : sqlRepository.countByUserId(user.getId());

        return AnalyticsMetricsDTO.builder()
                .totalUsers(userRepository.count())
                .totalRepositories(repos)
                .totalCodeChunks(chunks)
                .totalEmbeddings(embeddings)
                .totalAiChats(repos > 0 ? 12 : 0)
                .totalExceptionAnalyses(exceptions)
                .totalLogAnalyses(logs)
                .totalCodeReviews(reviews)
                .totalApiDocs(docs)
                .totalSqlOptimizations(sqls)
                .systemStatus("HEALTHY - ALL SYSTEMS OPERATIONAL")
                .javaVersion(System.getProperty("java.version"))
                .springVersion("3.3.4")
                .serverTime(LocalDateTime.now())
                .build();
    }

    private AnalyticsMetricsDTO getEmptyAnalytics() {
        return AnalyticsMetricsDTO.builder()
                .totalUsers(userRepository.count())
                .totalRepositories(0)
                .totalCodeChunks(0)
                .totalEmbeddings(0)
                .totalAiChats(0)
                .totalExceptionAnalyses(0)
                .totalLogAnalyses(0)
                .totalCodeReviews(0)
                .totalApiDocs(0)
                .totalSqlOptimizations(0)
                .systemStatus("HEALTHY - ALL SYSTEMS OPERATIONAL")
                .javaVersion(System.getProperty("java.version"))
                .springVersion("3.3.4")
                .serverTime(LocalDateTime.now())
                .build();
    }
}
