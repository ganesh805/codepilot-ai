package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExceptionAnalysisResponse {
    private String uuid;
    private String exceptionType;
    private String errorMessage;
    private String severity; // Critical, High, Medium, Low
    private int confidenceScore; // 0-100%
    private String confidenceReason;
    private String rootCauseSummary;
    private String estimatedFixTime;
    private String productionImpact;
    private String mergeRisk;

    private String rootCauseFile;
    private String rootCauseClass;
    private String rootCauseMethod;
    private Integer rootCauseLineNumber;

    private List<String> evidenceList;
    private Map<String, Integer> possibleCausesMap;
    private String businessImpact;
    private String recommendedFix;
    private String fixedCodeExample;
    private List<String> debugChecklist;
    private List<String> relatedTechnologies;
    private List<String> preventiveRecommendations;
    private List<String> learningResources;
    private List<String> timelineSteps;
    private long analysisDurationMs;

    private String fullReportMarkdown;
    private List<CodeCitation> matchedCitations;
    private LocalDateTime createdAt;
}
