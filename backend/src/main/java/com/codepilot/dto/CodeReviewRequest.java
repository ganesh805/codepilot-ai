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
public class CodeReviewRequest {

    @NotBlank(message = "Git diff content cannot be empty")
    private String gitDiff;

    private String prTitle;
    private String repositoryUuid;
}
