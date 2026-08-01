package com.codepilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeCitation {
    private String filePath;
    private String fileName;
    private String language;
    private int startLine;
    private int endLine;
    private double similarityScore;
    private String content;
}
