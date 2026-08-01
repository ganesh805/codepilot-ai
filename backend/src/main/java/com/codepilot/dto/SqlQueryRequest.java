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
public class SqlQueryRequest {

    @NotBlank(message = "Raw SQL query cannot be empty")
    private String rawSql;

    private String repositoryUuid;
}
