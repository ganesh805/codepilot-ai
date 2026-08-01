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
public class LogAnalysisResponse {
    private String uuid;
    private String fileName;
    private int totalLines;
    private int errorCount;
    private int warnCount;
    private int infoCount;
    private String summary;
    private List<String> flaggedErrorLines;
    private LocalDateTime createdAt;
}
