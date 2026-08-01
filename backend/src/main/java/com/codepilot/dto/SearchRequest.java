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
public class SearchRequest {

    @NotBlank(message = "Search query cannot be empty")
    private String query;

    @Builder.Default
    private int topK = 5;
}
