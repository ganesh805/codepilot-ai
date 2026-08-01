package com.codepilot.service;

import com.codepilot.dto.LogAnalysisResponse;
import com.codepilot.entity.LogAnalysis;
import com.codepilot.entity.User;
import com.codepilot.repository.LogAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogAnalyzerEngine {

    private static final Logger log = LoggerFactory.getLogger(LogAnalyzerEngine.class);

    private final LogAnalysisRepository logRepository;

    public LogAnalyzerEngine(LogAnalysisRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public LogAnalysisResponse analyzeLogContent(User user, String fileName, String logContent) {
        if (logContent == null || logContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Log content cannot be empty");
        }

        List<String> lines = Arrays.asList(logContent.split("\\r?\\n"));
        int totalLines = lines.size();

        int errorCount = 0;
        int warnCount = 0;
        int infoCount = 0;

        List<String> flaggedErrors = new ArrayList<>();

        for (String line : lines) {
            String upper = line.toUpperCase();
            if (upper.contains("ERROR") || upper.contains("FATAL") || upper.contains("EXCEPTION")) {
                errorCount++;
                if (flaggedErrors.size() < 10) {
                    flaggedErrors.add(line);
                }
            } else if (upper.contains("WARN") || upper.contains("WARNING")) {
                warnCount++;
            } else if (upper.contains("INFO")) {
                infoCount++;
            }
        }

        String summary = generateHealthSummary(totalLines, errorCount, warnCount, infoCount, flaggedErrors);

        LogAnalysis entity = LogAnalysis.builder()
                .user(user)
                .fileName(fileName != null ? fileName : "app.log")
                .totalLines(totalLines)
                .errorCount(errorCount)
                .warnCount(warnCount)
                .infoCount(infoCount)
                .summary(summary)
                .build();

        LogAnalysis saved = logRepository.save(entity);

        return LogAnalysisResponse.builder()
                .uuid(saved.getUuid())
                .fileName(saved.getFileName())
                .totalLines(saved.getTotalLines())
                .errorCount(saved.getErrorCount())
                .warnCount(saved.getWarnCount())
                .infoCount(saved.getInfoCount())
                .summary(saved.getSummary())
                .flaggedErrorLines(flaggedErrors)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LogAnalysisResponse> getUserLogHistory(User user) {
        return logRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateHealthSummary(int total, int errors, int warns, int infos, List<String> errorLines) {
        StringBuilder sb = new StringBuilder();
        double errorRate = total > 0 ? ((double) errors / total) * 100.0 : 0.0;

        if (errors > 0) {
            sb.append(String.format("### ⚠️ System Status: CRITICAL ATTENTION REQUIRED (Error Rate: %.1f%%)\n\n", errorRate));
            sb.append(String.format("- Analyzed **%d** total log lines. Detected **%d ERRORs**, **%d WARNs**, and **%d INFOs**.\n", total, errors, warns, infos));
            sb.append("- **Primary Issues Detected**:\n");
            for (String err : errorLines.stream().limit(3).collect(Collectors.toList())) {
                sb.append(String.format("  - `%s`\n", err));
            }
            sb.append("\n**Mitigation Recommendation**: Inspect database connection pools, check external API timeouts, and review authentication middleware filters.");
        } else if (warns > 0) {
            sb.append("### 🟡 System Status: STABLE WITH WARNINGS\n\n");
            sb.append(String.format("- Analyzed **%d** total log lines with **0 ERRORs** and **%d WARNs**.\n", total, warns));
            sb.append("- System is operational but contains minor warning spikes.");
        } else {
            sb.append("### ✅ System Status: HEALTHY & OPERATIONAL\n\n");
            sb.append(String.format("- Analyzed **%d** total log lines. All services initialized smoothly with zero error signatures.", total));
        }

        return sb.toString();
    }

    private LogAnalysisResponse mapToResponse(LogAnalysis entity) {
        return LogAnalysisResponse.builder()
                .uuid(entity.getUuid())
                .fileName(entity.getFileName())
                .totalLines(entity.getTotalLines())
                .errorCount(entity.getErrorCount())
                .warnCount(entity.getWarnCount())
                .infoCount(entity.getInfoCount())
                .summary(entity.getSummary())
                .flaggedErrorLines(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
