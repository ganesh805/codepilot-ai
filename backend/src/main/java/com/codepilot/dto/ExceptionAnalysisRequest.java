package com.codepilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExceptionAnalysisRequest {

    @NotBlank(message = "Stack trace cannot be empty")
    private String stackTrace;

    private String repositoryUuid;
}
