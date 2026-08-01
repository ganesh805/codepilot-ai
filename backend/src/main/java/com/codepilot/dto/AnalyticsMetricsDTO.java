package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsMetricsDTO {
    private long totalUsers;
    private long totalRepositories;
    private long totalCodeChunks;
    private long totalEmbeddings;
    private long totalAiChats;
    private long totalExceptionAnalyses;
    private long totalLogAnalyses;
    private long totalCodeReviews;
    private long totalApiDocs;
    private long totalSqlOptimizations;
    private String systemStatus;
    private String javaVersion;
    private String springVersion;
    private LocalDateTime serverTime;
}
