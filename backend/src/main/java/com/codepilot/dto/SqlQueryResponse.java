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
public class SqlQueryResponse {
    private String uuid;
    private String rawSql;
    private String optimizedSql;
    private String indexingDdl;
    private int performanceGainPct;
    private String analysisSummary;
    private List<String> detectedAntiPatterns;
    private LocalDateTime createdAt;
}
