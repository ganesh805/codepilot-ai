package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeReviewResponse {
    private String uuid;
    private String prTitle;
    private int qualityScore;
    private int securityScore;
    private int codeQualityScore;
    private int maintainabilityScore;
    private int performanceScore;
    private int bestPracticeScore;
    private int securityIssuesCount;
    private String mergeRecommendation;
    private long reviewDurationMs;

    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;

    private String summary;
    private List<String> securityAlerts;
    private List<String> improvements;
    private List<String> performanceAlerts;
    private List<String> maintainabilityAlerts;
    private List<String> bestPracticeAlerts;
    private List<String> positiveObservations;
    private List<String> prioritizedRecommendations;

    private LocalDateTime createdAt;
}
