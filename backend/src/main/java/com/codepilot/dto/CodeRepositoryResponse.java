package com.codepilot.dto;

import com.codepilot.entity.ImportType;
import com.codepilot.entity.RepositoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRepositoryResponse {
    private String uuid;
    private String name;
    private String owner;
    private String gitUrl;
    private ImportType importType;
    private String defaultBranch;
    private int fileCount;
    private long totalSizeBytes;
    private RepositoryStatus status;
    private LocalDateTime createdAt;
}
