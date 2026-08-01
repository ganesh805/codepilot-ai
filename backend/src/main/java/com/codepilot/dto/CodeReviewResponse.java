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
    private int securityIssuesCount;
    private String summary;
    private List<String> securityAlerts;
    private List<String> improvements;
    private LocalDateTime createdAt;
}
