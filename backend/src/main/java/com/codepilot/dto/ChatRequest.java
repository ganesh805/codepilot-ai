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
public class ChatRequest {

    @NotBlank(message = "Chat message cannot be empty")
    private String message;

    private String repositoryUuid;
    
    @Builder.Default
    private AiProvider aiProvider = AiProvider.GEMINI;
}
