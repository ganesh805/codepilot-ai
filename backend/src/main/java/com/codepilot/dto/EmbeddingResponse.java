package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingResponse {
    private String uuid;
    private String chunkUuid;
    private String filePath;
    private String fileName;
    private String language;
    private String qdrantPointId;
    private int vectorDimension;
    private String status;
    private LocalDateTime createdAt;
}
