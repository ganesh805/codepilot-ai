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
public class ExceptionAnalysisResponse {
    private String uuid;
    private String exceptionType;
    private String errorMessage;
    private String rootCause;
    private String suggestedFix;
    private List<CodeCitation> matchedCitations;
    private LocalDateTime createdAt;
}
