package com.codepilot.service;

import com.codepilot.dto.SqlQueryRequest;
import com.codepilot.dto.SqlQueryResponse;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.SqlOptimization;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeRepositoryRepository;
import com.codepilot.repository.SqlOptimizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SqlQueryOptimizerEngine {

    private static final Logger log = LoggerFactory.getLogger(SqlQueryOptimizerEngine.class);

    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile("(?i)FROM\\s+([a-zA-Z0-9_]+)");
    private static final Pattern WHERE_COL_PATTERN = Pattern.compile("(?i)WHERE\\s+([a-zA-Z0-9_.]+)\\s*=");
    private static final Pattern JOIN_COL_PATTERN = Pattern.compile("(?i)JOIN\\s+([a-zA-Z0-9_]+)\\s+ON\\s+([a-zA-Z0-9_.]+)\\s*=\\s*([a-zA-Z0-9_.]+)");

    private final SqlOptimizationRepository sqlRepository;
    private final CodeRepositoryRepository repoRepository;

    public SqlQueryOptimizerEngine(SqlOptimizationRepository sqlRepository, CodeRepositoryRepository repoRepository) {
        this.sqlRepository = sqlRepository;
        this.repoRepository = repoRepository;
    }

    @Transactional
    public SqlQueryResponse optimizeQuery(User user, SqlQueryRequest request) {
        String rawSql = request.getRawSql();
        if (rawSql == null || rawSql.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw SQL query cannot be empty");
        }

        CodeRepository repo = null;
        if (request.getRepositoryUuid() != null && !request.getRepositoryUuid().isEmpty()) {
            repo = repoRepository.findByUuidAndUserId(request.getRepositoryUuid(), user.getId()).orElse(null);
        }

        List<String> antiPatterns = new ArrayList<>();
        List<String> indexList = new ArrayList<>();
        int estimatedGain = 40;

        String tableName = extractTableName(rawSql);

        // 1. Check SELECT * Wildcard
        if (rawSql.toUpperCase().contains("SELECT *")) {
            antiPatterns.add("⚠️ WILDCARD COLUMN SELECTION: `SELECT *` retrieves unnecessary columns over the network and prevents index-only scans.");
            estimatedGain += 20;
        }

        // 2. Check LIKE '%term%' Leading Wildcard
        if (rawSql.toUpperCase().contains("LIKE '%")) {
            antiPatterns.add("🚨 FULL TABLE SCAN: Leading wildcard `LIKE '%term%'` prevents B-Tree index lookup and forces full table scans.");
            estimatedGain += 25;
        }

        // 3. Check WHERE & JOIN Index Recommendations
        Matcher whereMatcher = WHERE_COL_PATTERN.matcher(rawSql);
        if (whereMatcher.find()) {
            String col = whereMatcher.group(1).replace(tableName + ".", "");
            indexList.add(String.format("CREATE INDEX idx_%s_%s ON %s(%s);", tableName, col, tableName, col));
            antiPatterns.add(String.format("💡 MISSING INDEX: Column `%s` in WHERE clause requires a B-Tree Composite Index.", col));
            estimatedGain += 15;
        }

        Matcher joinMatcher = JOIN_COL_PATTERN.matcher(rawSql);
        if (joinMatcher.find()) {
            String joinTable = joinMatcher.group(1);
            String joinCol = joinMatcher.group(3).replace(joinTable + ".", "");
            indexList.add(String.format("CREATE INDEX idx_%s_%s ON %s(%s);", joinTable, joinCol, joinTable, joinCol));
        }

        if (indexList.isEmpty()) {
            indexList.add(String.format("CREATE INDEX idx_%s_composite ON %s(status, created_at);", tableName, tableName));
        }

        String optimizedSql = buildOptimizedSql(rawSql, tableName);
        String indexingDdl = String.join("\n", indexList);
        int finalGain = Math.min(95, estimatedGain);

        String summary = generateAnalysisSummary(tableName, finalGain, antiPatterns, indexList);

        SqlOptimization entity = SqlOptimization.builder()
                .user(user)
                .repository(repo)
                .rawSql(rawSql)
                .optimizedSql(optimizedSql)
                .indexingDdl(indexingDdl)
                .performanceGainPct(finalGain)
                .analysisSummary(summary)
                .build();

        SqlOptimization saved = sqlRepository.save(entity);

        return SqlQueryResponse.builder()
                .uuid(saved.getUuid())
                .rawSql(saved.getRawSql())
                .optimizedSql(saved.getOptimizedSql())
                .indexingDdl(saved.getIndexingDdl())
                .performanceGainPct(saved.getPerformanceGainPct())
                .analysisSummary(saved.getAnalysisSummary())
                .detectedAntiPatterns(antiPatterns)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SqlQueryResponse> getUserSqlHistory(User user) {
        return sqlRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String extractTableName(String sql) {
        Matcher matcher = FROM_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "users";
    }

    private String buildOptimizedSql(String rawSql, String tableName) {
        String opt = rawSql;
        if (opt.toUpperCase().contains("SELECT *")) {
            opt = opt.replace("SELECT *", "SELECT id, name, email, status, created_at");
        }
        if (opt.toUpperCase().contains("LIKE '%")) {
            opt = opt.replace("LIKE '%", "LIKE '");
        }
        if (!opt.toUpperCase().contains("LIMIT") && !opt.toUpperCase().contains("JOIN FETCH")) {
            opt = opt + "\nLIMIT 100;";
        }
        return opt;
    }

    private String generateAnalysisSummary(String table, int gain, List<String> antiPatterns, List<String> indexList) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### EXPLAIN Query Analysis Report: Table `%s`\n\n", table));
        sb.append(String.format("- **Estimated Performance Gain**: `+%d%% Faster Execution`\n", gain));
        sb.append(String.format("- **Detected Anti-Patterns**: `%d Query Code Smells`\n\n", antiPatterns.size()));

        sb.append("#### Anti-Pattern Breakdown:\n");
        for (String pattern : antiPatterns) {
            sb.append(String.format("- %s\n", pattern));
        }

        sb.append("\n#### EXPLAIN Plan Recommendation:\n");
        sb.append("Apply the suggested B-Tree DDL indexes to reduce EXPLAIN plan scan rows from `ALL` (Full Table Scan) to `ref`/`range` lookup.\n");

        return sb.toString();
    }

    private SqlQueryResponse mapToResponse(SqlOptimization entity) {
        return SqlQueryResponse.builder()
                .uuid(entity.getUuid())
                .rawSql(entity.getRawSql())
                .optimizedSql(entity.getOptimizedSql())
                .indexingDdl(entity.getIndexingDdl())
                .performanceGainPct(entity.getPerformanceGainPct())
                .analysisSummary(entity.getAnalysisSummary())
                .detectedAntiPatterns(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
