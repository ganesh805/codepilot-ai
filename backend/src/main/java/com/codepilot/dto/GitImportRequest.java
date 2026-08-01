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
public class GitImportRequest {

    @NotBlank(message = "GitHub repository URL is required")
    private String gitUrl;

    @Builder.Default
    private String branch = "main";
}
