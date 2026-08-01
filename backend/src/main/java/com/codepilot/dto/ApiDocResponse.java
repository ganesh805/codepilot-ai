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
public class ApiDocResponse {
    private String uuid;
    private String repoName;
    private int totalEndpoints;
    private String markdownSpec;
    private String openapiJson;
    private List<EndpointSummaryDTO> endpoints;
    private LocalDateTime createdAt;
}
