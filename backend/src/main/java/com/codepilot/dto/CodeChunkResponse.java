package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChunkResponse {
    private String uuid;
    private String filePath;
    private String fileName;
    private String language;
    private int chunkIndex;
    private int startLine;
    private int endLine;
    private int tokenCount;
    private String content;
}
