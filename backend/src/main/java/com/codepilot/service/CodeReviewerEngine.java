package com.codepilot.service;

import com.codepilot.dto.CodeReviewRequest;
import com.codepilot.dto.CodeReviewResponse;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.CodeReview;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeRepositoryRepository;
import com.codepilot.repository.CodeReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CodeReviewerEngine {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewerEngine.class);

    private final CodeReviewRepository reviewRepository;
    private final CodeRepositoryRepository repoRepository;

    public CodeReviewerEngine(CodeReviewRepository reviewRepository, CodeRepositoryRepository repoRepository) {
        this.reviewRepository = reviewRepository;
        this.repoRepository = repoRepository;
    }

    @Transactional
    public CodeReviewResponse reviewDiff(User user, CodeReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String diff = request.getGitDiff();
        if (diff == null || diff.trim().isEmpty()) {
            throw new IllegalArgumentException("Git diff content cannot be empty");
        }

        String prTitle = request.getPrTitle() != null && !request.getPrTitle().trim().isEmpty()
                ? request.getPrTitle().trim()
                : "Pull Request Review";

        CodeRepository repo = null;
        if (request.getRepositoryUuid() != null && !request.getRepositoryUuid().isEmpty()) {
            repo = repoRepository.findByUuidAndUserId(request.getRepositoryUuid(), user.getId()).orElse(null);
        }

        List<String> securityAlerts = new ArrayList<>();
        List<String> codeQualityIssues = new ArrayList<>();
        List<String> performanceIssues = new ArrayList<>();
        List<String> maintainabilityIssues = new ArrayList<>();
        List<String> bestPracticeAlerts = new ArrayList<>();
        List<String> positiveObservations = new ArrayList<>();
        List<String> prioritizedRecommendations = new ArrayList<>();

        Set<String> seenIssues = new HashSet<>();

        String[] lines = diff.split("\\r?\\n");
        int currentLineNum = 0;

        for (String line : lines) {
            currentLineNum++;
            String trimmed = line.trim();

            if (trimmed.startsWith("@@")) {
                continue;
            }

            if (trimmed.startsWith("+") && !trimmed.startsWith("+++")) {
                String addedCode = trimmed.substring(1).trim();

                // 1. FILTER COMMENTS & DOCUMENTATION (Ignore non-executable text)
                if (isCommentOrDoc(addedCode)) {
                    continue;
                }

                String codeUpper = addedCode.toUpperCase();

                // 2. SECURITY VULNERABILITIES (OWASP Top 10 2021 + CWE)

                // OWASP A03:2021 - Injection (CWE-89: SQL Injection)
                if ((codeUpper.contains("SELECT ") || codeUpper.contains("UPDATE ") || codeUpper.contains("DELETE ")) && addedCode.contains("+")) {
                    String key = "SQL_INJECTION:" + addedCode;
                    if (seenIssues.add(key)) {
                        securityAlerts.add(String.format(
                                "🔴 [CRITICAL] OWASP A03:2021 - Injection (CWE-89: SQL Injection)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Why Vulnerable: Dynamic string concatenation allows malicious SQL query injection.\n" +
                                "  • Business Impact: Attacker can compromise entire database contents, leak PII, or drop schemas.\n" +
                                "  • Exploitation: `admin' OR '1'='1` bypasses authentication.\n" +
                                "  • Fix: Use Parameterized PreparedStatement or Spring Data JPA `@Query`.\n" +
                                "  • Secure Example:\n" +
                                "    ```java\n" +
                                "    @Query(\"SELECT u FROM User u WHERE u.username = :username\")\n" +
                                "    User findByUsername(@Param(\"username\") String username);\n" +
                                "    ```",
                                currentLineNum, addedCode
                        ));
                    }
                }

                // OWASP A02:2021 - Cryptographic Failures (CWE-798: Use of Hardcoded Credentials)
                if ((codeUpper.contains("KEY =") || codeUpper.contains("SECRET =") || codeUpper.contains("PASSWORD =") || codeUpper.contains("TOKEN =")) 
                        && (addedCode.contains("\"") || addedCode.contains("'"))) {
                    String key = "HARDCODED_SECRET:" + addedCode;
                    if (seenIssues.add(key)) {
                        securityAlerts.add(String.format(
                                "🟠 [HIGH] OWASP A02:2021 - Cryptographic Failures (CWE-798: Use of Hard-coded Credentials)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Why Vulnerable: Storing secrets in source control exposes credentials to unauthorized developers & Git history.\n" +
                                "  • Business Impact: Complete compromise of cloud resources or JWT signature validation.\n" +
                                "  • Fix: Move credentials to Vault or Environment Variables (`@Value(\"${app.secret}\")`).\n" +
                                "  • Secure Example:\n" +
                                "    ```java\n" +
                                "    @Value(\"${jwt.secret.key}\")\n" +
                                "    private String jwtSecretKey;\n" +
                                "    ```",
                                currentLineNum, addedCode
                        ));
                    }
                }

                // OWASP A02:2021 - Cryptographic Failures (CWE-327: Broken Crypto Algorithm)
                if (codeUpper.contains("MD5") || codeUpper.contains("DES")) {
                    String key = "WEAK_CRYPTO:" + addedCode;
                    if (seenIssues.add(key)) {
                        securityAlerts.add(String.format(
                                "🟠 [HIGH] OWASP A02:2021 - Cryptographic Failures (CWE-327: Use of a Broken Cryptographic Algorithm)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Why Vulnerable: MD5/DES are cryptographically broken algorithms subject to rapid collision attacks.\n" +
                                "  • Business Impact: Passwords or signatures hashed with MD5 can be cracked in seconds via rainbow tables.\n" +
                                "  • Fix: Upgrade to `BCryptPasswordEncoder` or `Argon2`.\n" +
                                "  • Secure Example:\n" +
                                "    ```java\n" +
                                "    PasswordEncoder encoder = new BCryptPasswordEncoder();\n" +
                                "    String hash = encoder.encode(rawPassword);\n" +
                                "    ```",
                                currentLineNum, addedCode
                        ));
                    }
                }

                // OWASP A05:2021 - Security Misconfiguration (CWE-942: Permissive CORS)
                if (addedCode.contains("@CrossOrigin(\"*\")") || addedCode.contains("allowedOrigins(\"*\")")) {
                    String key = "PERMISSIVE_CORS:" + addedCode;
                    if (seenIssues.add(key)) {
                        securityAlerts.add(String.format(
                                "🟡 [MEDIUM] OWASP A05:2021 - Security Misconfiguration (CWE-942: Permissive CORS Wildcard)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Why Vulnerable: Wildcard `*` permits any malicious origin to execute authenticated requests.\n" +
                                "  • Fix: Restrict allowed origins to designated domains (`allowedOrigins(\"https://app.example.com\")`).",
                                currentLineNum, addedCode
                        ));
                    }
                }

                // 3. CODE QUALITY CHECKS
                if (addedCode.contains("throws Exception") || addedCode.contains("catch (Exception e)")) {
                    String key = "GENERIC_EXCEPTION:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: Catch or throw specific exception types (e.g. `UserNotFoundException`, `SQLException`) instead of generic `Exception`.", currentLineNum));
                    }
                }

                if (addedCode.contains("new String(") && !addedCode.contains("StandardCharsets")) {
                    String key = "NEW_STRING_BYTES:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: Explicitly specify `StandardCharsets.UTF_8` in byte-to-String conversions to prevent OS encoding bugs.", currentLineNum));
                    }
                }

                if (addedCode.contains("System.out.println") || addedCode.contains("console.log")) {
                    String key = "RAW_LOGGING:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: Replace console print statements with SLF4J Logger (`log.info(...)`).", currentLineNum));
                    }
                }

                // 4. PERFORMANCE CHECKS
                if (addedCode.contains("for (") || addedCode.contains("while (")) {
                    if (addedCode.contains("+=")) {
                        String key = "STRING_CONCAT_LOOP:" + addedCode;
                        if (seenIssues.add(key)) {
                            performanceIssues.add(String.format("⚡ Line %d: String concatenation in loop allocates redundant objects. Use `StringBuilder`.", currentLineNum));
                        }
                    }
                }

                // 5. MAINTAINABILITY CHECKS
                if (addedCode.length() > 140) {
                    String key = "LONG_LINE:" + addedCode;
                    if (seenIssues.add(key)) {
                        maintainabilityIssues.add(String.format("🔧 Line %d: Line length (%d chars) exceeds clean code readability boundary (120 chars).", currentLineNum, addedCode.length()));
                    }
                }

                // 6. BEST PRACTICE SUGGESTIONS
                if (addedCode.contains("new ResponseEntity") && !addedCode.contains(".ok(")) {
                    String key = "BEST_PRAC_RESPONSE";
                    if (seenIssues.add(key)) {
                        bestPracticeAlerts.add(String.format("💡 Line %d: Prefer Spring `ResponseEntity.ok(...)` builder methods over explicit `new ResponseEntity(...)` constructors.", currentLineNum));
                    }
                }

                // 7. POSITIVE OBSERVATIONS
                if (addedCode.contains("BCryptPasswordEncoder") || addedCode.contains("passwordEncoder.matches")) {
                    String key = "POS_BCRYPT";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✓ Using industry-standard BCrypt password hashing.");
                    }
                }
                if (addedCode.contains("PreparedStatement") || addedCode.contains("jdbcTemplate.query(") || addedCode.contains("findBy")) {
                    String key = "POS_PREPARED";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✓ Utilizing Parameterized queries preventing SQL Injection.");
                    }
                }
                if (addedCode.contains("@Autowired") || (addedCode.contains("final ") && addedCode.contains("Repository"))) {
                    String key = "POS_DI";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✓ Following Spring Dependency Injection & immutability practices.");
                    }
                }
            }
        }

        if (positiveObservations.isEmpty()) {
            positiveObservations.add("No notable best practices detected.");
        }

        // Count Severities
        int criticalCount = (int) securityAlerts.stream().filter(a -> a.contains("[CRITICAL]")).count();
        int highCount = (int) securityAlerts.stream().filter(a -> a.contains("[HIGH]")).count();
        int mediumCount = (int) securityAlerts.stream().filter(a -> a.contains("[MEDIUM]")).count();
        int lowCount = (int) securityAlerts.stream().filter(a -> a.contains("[LOW]")).count();

        // 5-FACTOR WEIGHTED SCORING ENGINE (Security 40%, Quality 25%, Maintainability 15%, Performance 10%, Best Practices 10%)
        int secScore = Math.max(0, 100 - (criticalCount * 40) - (highCount * 25) - (mediumCount * 15) - (lowCount * 5));
        int qualScore = Math.max(0, 100 - (codeQualityIssues.size() * 10));
        int mainScore = Math.max(0, 100 - (maintainabilityIssues.size() * 10));
        int perfScore = Math.max(0, 100 - (performanceIssues.size() * 12));
        int bestScore = Math.max(0, 100 - (bestPracticeAlerts.size() * 10));

        int overallScore = (int) Math.round((secScore * 0.40) + (qualScore * 0.25) + (mainScore * 0.15) + (perfScore * 0.10) + (bestScore * 0.10));
        overallScore = Math.max(15, overallScore);

        // DETERMINISTIC MERGE DECISION
        String mergeRecommendation;
        if (criticalCount > 0) {
            mergeRecommendation = "🔴 BLOCK MERGE";
        } else if (highCount > 0) {
            mergeRecommendation = "🟠 REQUEST CHANGES";
        } else if (mediumCount > 0 || codeQualityIssues.size() > 2) {
            mergeRecommendation = "🟡 APPROVE WITH SUGGESTIONS";
        } else {
            mergeRecommendation = "🟢 APPROVE";
        }

        // PRIORITIZED RECOMMENDATIONS
        if (criticalCount > 0 || highCount > 0) {
            prioritizedRecommendations.add("Priority 1 (Critical): Eliminate Security Vulnerabilities before merging.");
        }
        if (!codeQualityIssues.isEmpty()) {
            prioritizedRecommendations.add("Priority 2 (High): Address Code Quality & Exception Handling issues.");
        }
        if (!performanceIssues.isEmpty()) {
            prioritizedRecommendations.add("Priority 3 (Medium): Optimize loops and memory efficiency.");
        }
        if (prioritizedRecommendations.isEmpty()) {
            prioritizedRecommendations.add("Priority 1: Code satisfies all enterprise quality standards.");
        }

        long durationMs = System.currentTimeMillis() - startTime;

        String summary = generateStructuredReviewReport(prTitle, overallScore, mergeRecommendation, securityAlerts, codeQualityIssues, performanceIssues, maintainabilityIssues, positiveObservations, prioritizedRecommendations);

        CodeReview entity = CodeReview.builder()
                .user(user)
                .repository(repo)
                .prTitle(prTitle)
                .gitDiff(diff)
                .qualityScore(overallScore)
                .securityIssuesCount(securityAlerts.size())
                .summary(summary)
                .build();

        CodeReview saved = reviewRepository.save(entity);

        return CodeReviewResponse.builder()
                .uuid(saved.getUuid())
                .prTitle(saved.getPrTitle())
                .qualityScore(overallScore)
                .securityScore(secScore)
                .codeQualityScore(qualScore)
                .maintainabilityScore(mainScore)
                .performanceScore(perfScore)
                .bestPracticeScore(bestScore)
                .securityIssuesCount(securityAlerts.size())
                .mergeRecommendation(mergeRecommendation)
                .reviewDurationMs(durationMs)
                .criticalCount(criticalCount)
                .highCount(highCount)
                .mediumCount(mediumCount)
                .lowCount(lowCount)
                .summary(saved.getSummary())
                .securityAlerts(securityAlerts)
                .improvements(codeQualityIssues)
                .performanceAlerts(performanceIssues)
                .maintainabilityAlerts(maintainabilityIssues)
                .bestPracticeAlerts(bestPracticeAlerts)
                .positiveObservations(positiveObservations)
                .prioritizedRecommendations(prioritizedRecommendations)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CodeReviewResponse> getUserReviewHistory(User user) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean isCommentOrDoc(String code) {
        if (code.startsWith("//") || code.startsWith("/*") || code.startsWith("*") || code.startsWith("*/") 
                || code.startsWith("#") || code.startsWith("<!--") || code.startsWith("@param") || code.startsWith("@return")) {
            return true;
        }
        return false;
    }

    private String generateStructuredReviewReport(
            String title, int score, String mergeRec, 
            List<String> securityAlerts, List<String> qualityIssues, 
            List<String> perfIssues, List<String> mainIssues,
            List<String> positiveObs, List<String> prioritizedRecs) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### 📋 Executive Summary: **%s**\n\n", title));
        sb.append(String.format("- **Overall Score**: `%d / 100`\n", score));
        sb.append(String.format("- **Merge Recommendation**: **%s**\n\n", mergeRec));

        // 1. Security Findings
        sb.append("-----------------------------------\n");
        sb.append("### 🛡️ Security Findings\n\n");
        if (securityAlerts.isEmpty()) {
            sb.append("🟢 **No OWASP security vulnerabilities or credential leaks detected.**\n\n");
        } else {
            for (String alert : securityAlerts) {
                sb.append(String.format("%s\n\n", alert));
            }
        }

        // 2. Code Quality Findings
        sb.append("-----------------------------------\n");
        sb.append("### 🧹 Code Quality Findings\n\n");
        if (qualityIssues.isEmpty()) {
            sb.append("🟢 **No code quality issues detected.**\n\n");
        } else {
            for (String issue : qualityIssues) {
                sb.append(String.format("- %s\n", issue));
            }
            sb.append("\n");
        }

        // 3. Performance Findings
        sb.append("-----------------------------------\n");
        sb.append("### ⚡ Performance Findings\n\n");
        if (perfIssues.isEmpty()) {
            sb.append("🟢 **No performance bottlenecks detected.**\n\n");
        } else {
            for (String perf : perfIssues) {
                sb.append(String.format("- %s\n", perf));
            }
            sb.append("\n");
        }

        // 4. Maintainability Findings
        sb.append("-----------------------------------\n");
        sb.append("### 🔧 Maintainability Findings\n\n");
        if (mainIssues.isEmpty()) {
            sb.append("🟢 **No maintainability concerns detected.**\n\n");
        } else {
            for (String main : mainIssues) {
                sb.append(String.format("- %s\n", main));
            }
            sb.append("\n");
        }

        // 5. Positive Observations
        sb.append("-----------------------------------\n");
        sb.append("### ✨ Positive Observations\n\n");
        for (String pos : positiveObs) {
            sb.append(String.format("- %s\n", pos));
        }
        sb.append("\n");

        // 6. Prioritized Recommendations
        sb.append("-----------------------------------\n");
        sb.append("### 📋 Prioritized Recommendations\n\n");
        for (String rec : prioritizedRecs) {
            sb.append(String.format("- **%s**\n", rec));
        }
        sb.append("\n-----------------------------------\n");
        sb.append(String.format("### 🏁 Final Verdict: **%s**\n", mergeRec));

        return sb.toString();
    }

    private CodeReviewResponse mapToResponse(CodeReview entity) {
        return CodeReviewResponse.builder()
                .uuid(entity.getUuid())
                .prTitle(entity.getPrTitle())
                .qualityScore(entity.getQualityScore())
                .securityIssuesCount(entity.getSecurityIssuesCount())
                .mergeRecommendation(entity.getQualityScore() >= 80 ? "🟢 APPROVE" : "🔴 BLOCK MERGE")
                .summary(entity.getSummary())
                .securityAlerts(new ArrayList<>())
                .improvements(new ArrayList<>())
                .performanceAlerts(new ArrayList<>())
                .maintainabilityAlerts(new ArrayList<>())
                .bestPracticeAlerts(new ArrayList<>())
                .positiveObservations(new ArrayList<>())
                .prioritizedRecommendations(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
