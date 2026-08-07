package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeOptimizerRequest {

    private String code;
    private String language; // Optional explicit language hint
    private String repositoryUuid; // Optional repo link
}
