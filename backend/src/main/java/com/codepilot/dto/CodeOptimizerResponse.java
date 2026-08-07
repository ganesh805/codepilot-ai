package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeOptimizerResponse {

    private String uuid;
    private String detectedLanguage;
    private String detectedFramework;
    private String optimizationConfidence; // High, Medium, Already Optimal
    private String optimizationLevel; // LEVEL 1 to LEVEL 4

    private String algorithmBefore;
    private String algorithmAfter;
    private String dataStructureBefore;
    private String dataStructureAfter;

    private String timeComplexityBefore;
    private String timeComplexityAfter;
    private String spaceComplexityBefore;
    private String spaceComplexityAfter;

    private String theoreticalImprovement; // e.g. "Reduces algorithmic complexity from O(N²) to O(N)"
    private List<String> bottlenecks;
    private String rawCode;
    private String optimizedCode;
    private String whyBetter;
    private String tradeOffs;
    private String whenNotToUse;
    private String correctnessNotes;
    private boolean isAlreadyOptimal;

    private String fullReportMarkdown;
    private LocalDateTime createdAt;
}
