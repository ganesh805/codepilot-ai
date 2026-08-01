package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointSummaryDTO {
    private String httpMethod;
    private String path;
    private String controllerClass;
    private String methodName;
    private String filePath;
    private int startLine;
    private int endLine;
    private String sourceCodeSnippet;
}
