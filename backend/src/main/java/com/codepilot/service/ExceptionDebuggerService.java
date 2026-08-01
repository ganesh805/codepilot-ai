package com.codepilot.service;

import com.codepilot.dto.CodeCitation;
import com.codepilot.dto.ExceptionAnalysisRequest;
import com.codepilot.dto.ExceptionAnalysisResponse;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.ExceptionAnalysis;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeRepositoryRepository;
import com.codepilot.repository.ExceptionAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExceptionDebuggerService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionDebuggerService.class);

    private static final Pattern EXCEPTION_TYPE_PATTERN = Pattern.compile("([a-zA-Z0-9_.]+(?:Exception|Error)):?\\s*(.*)");
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile("at\\s+([a-zA-Z0-9_.$]+)\\.([a-zA-Z0-9_]+)\\(([^:]+):([0-9]+)\\)");

    private final ExceptionAnalysisRepository analysisRepository;
    private final CodeRepositoryRepository repoRepository;

    public ExceptionDebuggerService(
            ExceptionAnalysisRepository analysisRepository,
            CodeRepositoryRepository repoRepository) {
        this.analysisRepository = analysisRepository;
        this.repoRepository = repoRepository;
    }

    @Transactional
    public ExceptionAnalysisResponse analyzeStackTrace(User user, ExceptionAnalysisRequest request) {
        String trace = request.getStackTrace();
        if (trace == null || trace.trim().isEmpty()) {
            throw new IllegalArgumentException("Stack trace cannot be empty");
        }

        String exceptionType = extractExceptionType(trace);
        String errorMessage = extractErrorMessage(trace);

        CodeRepository repo = null;
        if (request.getRepositoryUuid() != null && !request.getRepositoryUuid().isEmpty()) {
            repo = repoRepository.findByUuidAndUserId(request.getRepositoryUuid(), user.getId()).orElse(null);
        }

        String rootCause = buildRootCauseDiagnosis(exceptionType, errorMessage, trace);
        String suggestedFix = buildSuggestedFixCode(exceptionType, errorMessage);

        ExceptionAnalysis entity = ExceptionAnalysis.builder()
                .user(user)
                .repository(repo)
                .exceptionType(exceptionType)
                .errorMessage(errorMessage)
                .stackTrace(trace)
                .rootCause(rootCause)
                .suggestedFix(suggestedFix)
                .build();

        ExceptionAnalysis saved = analysisRepository.save(entity);

        return ExceptionAnalysisResponse.builder()
                .uuid(saved.getUuid())
                .exceptionType(saved.getExceptionType())
                .errorMessage(saved.getErrorMessage())
                .rootCause(saved.getRootCause())
                .suggestedFix(saved.getSuggestedFix())
                .matchedCitations(new ArrayList<>())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ExceptionAnalysisResponse> getUserAnalysisHistory(User user) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String extractExceptionType(String trace) {
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(trace);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "RuntimeEngineException";
    }

    private String extractErrorMessage(String trace) {
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(trace);
        if (matcher.find() && matcher.groupCount() >= 2 && !matcher.group(2).isEmpty()) {
            return matcher.group(2);
        }
        String[] lines = trace.split("\n");
        return lines.length > 0 ? lines[0] : "Unhandled Exception Occurred";
    }

    private String buildRootCauseDiagnosis(String type, String message, String trace) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Root Cause Diagnosis: %s\n\n", type));

        if (type.contains("NullPointerException")) {
            sb.append("**Root Cause**: An attempt was made to invoke a method or dereference an object variable (`.`) on an uninitialized `null` reference.\n");
            sb.append("**Location**: Check line number indicated in top stack frame. Common cause is missing Spring `@Autowired` bean injection, uninitialized JPA relation, or optional field value.\n");
        } else if (type.contains("ExpiredJwtException")) {
            sb.append("**Root Cause**: The JWT authentication Bearer token timestamp (`exp` claim) has passed its valid lifetime window.\n");
            sb.append("**Location**: `JwtTokenProvider.java` / `JwtAuthenticationFilter.java`. Client sent an expired session token.\n");
        } else if (type.contains("SecurityException") || type.contains("ZipSlip")) {
            sb.append("**Root Cause**: Attempted relative directory path traversal (`../`) during archive extraction.\n");
            sb.append("**Location**: `RepositoryImportService.java` zip slip protection check.\n");
        } else {
            sb.append(String.format("**Root Cause**: Handled runtime execution failure: `%s`.\n", message));
        }

        return sb.toString();
    }

    private String buildSuggestedFixCode(String type, String message) {
        if (type.contains("NullPointerException")) {
            return """
                   // Suggested Fix: Add Explicit Null Guard & Spring Optional Handling
                   if (user == null || user.getRoles() == null) {
                       throw new IllegalArgumentException("Target user or role assignment cannot be null");
                   }
                   """;
        } else if (type.contains("ExpiredJwtException")) {
            return """
                   // Suggested Fix: Handle ExpiredJwtException in JwtAuthenticationFilter
                   try {
                       String token = getJwtFromRequest(request);
                       if (token != null && tokenProvider.validateToken(token)) {
                           // Authenticate user...
                       }
                   } catch (ExpiredJwtException ex) {
                       log.warn("JWT Token expired: {}", ex.getMessage());
                       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                       response.getWriter().write("{\\"error\\": \\"JWT Session Expired\\"}");
                       return;
                   }
                   """;
        } else {
            return """
                   // Suggested Fix: Wrap execution block in Try-Catch handling
                   try {
                       // Execute target operation...
                   } catch (Exception ex) {
                       log.error("Operation failed: {}", ex.getMessage(), ex);
                       throw new CustomBusinessException("Failed operation: " + ex.getMessage());
                   }
                   """;
        }
    }

    private ExceptionAnalysisResponse mapToResponse(ExceptionAnalysis entity) {
        return ExceptionAnalysisResponse.builder()
                .uuid(entity.getUuid())
                .exceptionType(entity.getExceptionType())
                .errorMessage(entity.getErrorMessage())
                .rootCause(entity.getRootCause())
                .suggestedFix(entity.getSuggestedFix())
                .matchedCitations(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
