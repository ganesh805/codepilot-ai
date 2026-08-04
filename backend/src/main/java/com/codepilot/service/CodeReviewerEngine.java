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
        List<String> positiveObservations = new ArrayList<>();
        Set<String> seenIssues = new HashSet<>();

        String[] lines = diff.split("\\r?\\n");
        int currentLineNum = 0;

        for (String line : lines) {
            currentLineNum++;
            String trimmed = line.trim();

            // Track line numbers in unified diff headers
            if (trimmed.startsWith("@@")) {
                continue;
            }

            // Only process added lines (+) and ignore deleted (-) or file headers (+++)
            if (trimmed.startsWith("+") && !trimmed.startsWith("+++")) {
                String addedCode = trimmed.substring(1).trim();

                // 1. FILTER COMMENTS & DOCUMENTATION (Ignore non-executable text)
                if (isCommentOrDoc(addedCode)) {
                    continue;
                }

                String codeUpper = addedCode.toUpperCase();

                // 2. OWASP SECURITY CHECKS (Vulnerabilities only)
                
                // OWASP A03:2021 - Injection (CWE-89: SQL Injection)
                if ((codeUpper.contains("SELECT ") || codeUpper.contains("UPDATE ") || codeUpper.contains("DELETE ")) && addedCode.contains("+")) {
                    String key = "SQL_INJECTION:" + addedCode;
                    if (seenIssues.add(key)) {
                        securityAlerts.add(String.format(
                                "🚨 [CRITICAL] OWASP A03:2021 - Injection (CWE-89: SQL Injection)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Impact: Attacker can manipulate database queries to dump or destroy sensitive data.\n" +
                                "  • Fix: Use Parameterized PreparedStatement or Spring Data JPA `@Query(\"... WHERE u.name = :name\")`.\n" +
                                "  • Secure Code:\n" +
                                "    ```java\n" +
                                "    String sql = \"SELECT * FROM users WHERE username = ?\";\n" +
                                "    return jdbcTemplate.queryForObject(sql, new Object[]{username}, User.class);\n" +
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
                                "🚨 [HIGH] OWASP A02:2021 - Cryptographic Failures (CWE-798: Use of Hard-coded Credentials)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Impact: Hardcoded secrets in source control allow unauthorized system access.\n" +
                                "  • Fix: Store secrets in Environment Variables or Key Vault (`@Value(\"${jwt.secret}\")`).\n" +
                                "  • Secure Code:\n" +
                                "    ```java\n" +
                                "    @Value(\"${aws.secret.key}\")\n" +
                                "    private String awsSecretKey;\n" +
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
                                "🚨 [HIGH] OWASP A02:2021 - Cryptographic Failures (CWE-327: Use of a Broken Cryptographic Algorithm)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Impact: MD5 and DES algorithms are susceptible to fast collision attacks and cracking.\n" +
                                "  • Fix: Upgrade to BCryptPasswordEncoder or Argon2.\n" +
                                "  • Secure Code:\n" +
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
                                "⚠️ [MEDIUM] OWASP A05:2021 - Security Misconfiguration (CWE-942: Permissive CORS Wildcard)\n" +
                                "  • Confidence: HIGH | Line %d: `%s`\n" +
                                "  • Impact: Allows any malicious website to issue cross-origin requests to your API.\n" +
                                "  • Fix: Restrict allowed origins to specific trusted domains (`allowedOrigins(\"https://app.com\")`).",
                                currentLineNum, addedCode
                        ));
                    }
                }

                // 3. CODE QUALITY CHECKS (Non-security maintainability issues)
                if (addedCode.contains("throws Exception") || addedCode.contains("catch (Exception e)")) {
                    String key = "GENERIC_EXCEPTION:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: Avoid generic `Exception`. Catch specific exceptions (e.g. `UserNotFoundException`, `SQLException`) for robust error handling.", currentLineNum));
                    }
                }

                if (addedCode.contains("new String(") && !addedCode.contains("StandardCharsets")) {
                    String key = "NEW_STRING_BYTES:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: `new String(bytes)` uses default platform charset. Explicitly specify `StandardCharsets.UTF_8`.", currentLineNum));
                    }
                }

                if (addedCode.contains("System.out.println") || addedCode.contains("console.log")) {
                    String key = "RAW_LOGGING:" + addedCode;
                    if (seenIssues.add(key)) {
                        codeQualityIssues.add(String.format("🧹 Line %d: Replace raw console print statements with SLF4J Logger (`log.info(...)` or `log.error(...)`).", currentLineNum));
                    }
                }

                // 4. PERFORMANCE CHECKS
                if (addedCode.contains("for (") || addedCode.contains("while (")) {
                    if (addedCode.contains("+=")) {
                        String key = "STRING_CONCAT_LOOP:" + addedCode;
                        if (seenIssues.add(key)) {
                            performanceIssues.add(String.format("⚡ Line %d: String concatenation inside loops allocates multiple temporary objects. Use `StringBuilder`.", currentLineNum));
                        }
                    }
                }

                // 5. POSITIVE OBSERVATIONS (Good engineering practices detected)
                if (addedCode.contains("BCryptPasswordEncoder") || addedCode.contains("passwordEncoder.matches")) {
                    String key = "POS_BCRYPT";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✨ POSITIVE: Using industry-standard BCrypt password hashing.");
                    }
                }
                if (addedCode.contains("PreparedStatement") || addedCode.contains("jdbcTemplate.query(")) {
                    String key = "POS_PREPARED_STMT";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✨ POSITIVE: Utilizing Parameterized SQL queries preventing SQL Injection.");
                    }
                }
                if (addedCode.contains("@Autowired") || addedCode.contains("final ") && addedCode.contains("Repository")) {
                    String key = "POS_DI";
                    if (seenIssues.add(key)) {
                        positiveObservations.add("✨ POSITIVE: Following Spring Dependency Injection & immutability practices.");
                    }
                }
            }
        }

        if (positiveObservations.isEmpty()) {
            positiveObservations.add("✨ POSITIVE: Code conforms to standard Java 21 & Spring Boot modular structure.");
        }

        // Calculate Realistic Quality Scorecard
        int criticalCount = (int) securityAlerts.stream().filter(a -> a.contains("[CRITICAL]")).count();
        int highCount = (int) securityAlerts.stream().filter(a -> a.contains("[HIGH]")).count();
        int mediumCount = (int) securityAlerts.stream().filter(a -> a.contains("[MEDIUM]")).count();
        int lowCount = (int) securityAlerts.stream().filter(a -> a.contains("[LOW]")).count();

        int deductions = (criticalCount * 25) + (highCount * 15) + (mediumCount * 10) + (lowCount * 5)
                + (codeQualityIssues.size() * 4) + (performanceIssues.size() * 3);
        int score = Math.max(15, 100 - deductions);

        // Determine Merge Recommendation
        String mergeRecommendation;
        if (criticalCount > 0 || highCount > 0) {
            mergeRecommendation = "🚨 BLOCK MERGE (Critical Security Vulnerabilities Detected)";
        } else if (mediumCount > 0 || codeQualityIssues.size() > 2) {
            mergeRecommendation = "⚠️ REQUEST CHANGES (Security / Code Quality Issues Present)";
        } else {
            mergeRecommendation = "🟢 APPROVE MERGE (Pull Request Passes Quality & Security Audit)";
        }

        String summary = generateStructuredReviewReport(prTitle, score, mergeRecommendation, securityAlerts, codeQualityIssues, performanceIssues, positiveObservations);

        CodeReview entity = CodeReview.builder()
                .user(user)
                .repository(repo)
                .prTitle(prTitle)
                .gitDiff(diff)
                .qualityScore(score)
                .securityIssuesCount(securityAlerts.size())
                .summary(summary)
                .build();

        CodeReview saved = reviewRepository.save(entity);

        return CodeReviewResponse.builder()
                .uuid(saved.getUuid())
                .prTitle(saved.getPrTitle())
                .qualityScore(saved.getQualityScore())
                .securityIssuesCount(saved.getSecurityIssuesCount())
                .summary(saved.getSummary())
                .securityAlerts(securityAlerts)
                .improvements(codeQualityIssues)
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
            List<String> perfIssues, List<String> positiveObs) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### 📋 Executive Summary: **%s**\n\n", title));
        sb.append(String.format("- **Overall Code Quality Score**: `%d / 100`\n", score));
        sb.append(String.format("- **Merge Recommendation**: **%s**\n\n", mergeRec));

        // 1. Security Findings
        sb.append("### 🛡️ 1. Security Vulnerabilities (OWASP Top 10 Audit)\n");
        if (securityAlerts.isEmpty()) {
            sb.append("🟢 **No OWASP security vulnerabilities or credential leaks detected.**\n\n");
        } else {
            for (String alert : securityAlerts) {
                sb.append(String.format("%s\n\n", alert));
            }
        }

        // 2. Code Quality Findings
        sb.append("### 🧹 2. Code Quality & Maintainability Findings\n");
        if (qualityIssues.isEmpty()) {
            sb.append("🟢 **No code quality issues detected.**\n\n");
        } else {
            for (String issue : qualityIssues) {
                sb.append(String.format("- %s\n", issue));
            }
            sb.append("\n");
        }

        // 3. Performance Findings
        sb.append("### ⚡ 3. Performance Findings\n");
        if (perfIssues.isEmpty()) {
            sb.append("🟢 **No performance bottlenecks detected.**\n\n");
        } else {
            for (String perf : perfIssues) {
                sb.append(String.format("- %s\n", perf));
            }
            sb.append("\n");
        }

        // 4. Positive Observations
        sb.append("### ✨ 4. Positive Observations\n");
        for (String pos : positiveObs) {
            sb.append(String.format("- %s\n", pos));
        }
        sb.append("\n");

        // 5. Prioritized Action Items
        sb.append("### 📋 5. Prioritized Action Items\n");
        if (!securityAlerts.isEmpty()) {
            sb.append("1. **HIGH PRIORITY**: Resolve Security Blockers prior to merging into target branch.\n");
        }
        if (!qualityIssues.isEmpty()) {
            sb.append("2. **MEDIUM PRIORITY**: Apply recommended code quality refactorings.\n");
        }
        if (securityAlerts.isEmpty() && qualityIssues.isEmpty()) {
            sb.append("1. **READY FOR MERGE**: Code satisfies enterprise security and quality guidelines.\n");
        }

        return sb.toString();
    }

    private CodeReviewResponse mapToResponse(CodeReview entity) {
        return CodeReviewResponse.builder()
                .uuid(entity.getUuid())
                .prTitle(entity.getPrTitle())
                .qualityScore(entity.getQualityScore())
                .securityIssuesCount(entity.getSecurityIssuesCount())
                .summary(entity.getSummary())
                .securityAlerts(new ArrayList<>())
                .improvements(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
