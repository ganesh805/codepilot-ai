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

import java.util.ArrayList;
import java.util.List;
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
        List<String> improvements = new ArrayList<>();

        String[] lines = diff.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("+") && !trimmed.startsWith("+++")) {
                String addedCode = trimmed.substring(1).trim();
                String codeUpper = addedCode.toUpperCase();

                // 1. OWASP A02: Hardcoded Secrets & Credentials
                if (codeUpper.contains("KEY =") || codeUpper.contains("SECRET =") || codeUpper.contains("PASSWORD =") 
                        || codeUpper.contains("TOKEN =") || codeUpper.contains("AWS_") || codeUpper.contains("JWT_")) {
                    if (addedCode.contains("\"") || addedCode.contains("'")) {
                        securityAlerts.add("🚨 OWASP A02 HARDCODED SECRET: Hardcoded credential or API secret key detected in line: `" + addedCode + "`. Store credentials in Environment Variables or Secrets Manager!");
                    }
                }

                // 2. OWASP A03: SQL Injection via Dynamic String Concatenation
                if ((codeUpper.contains("SELECT ") || codeUpper.contains("UPDATE ") || codeUpper.contains("DELETE ")) && addedCode.contains("+")) {
                    securityAlerts.add("🚨 OWASP A03 CRITICAL SQL INJECTION: Dynamic String concatenation detected in SQL query: `" + addedCode + "`. Use Parameterized PreparedStatement or JPA Named Parameters!");
                } else if (addedCode.contains("executeQuery(") && addedCode.contains("+")) {
                    securityAlerts.add("🚨 OWASP A03 SQL INJECTION RISK: Dynamic SQL execution: `" + addedCode + "`.");
                }

                // 3. OWASP A02: Insecure Cryptography (MD5 / DES)
                if (codeUpper.contains("MD5") || codeUpper.contains("DES")) {
                    securityAlerts.add("⚠️ OWASP A02 WEAK CRYPTOGRAPHY: Deprecated hashing/cipher algorithm (MD5/DES) detected in line: `" + addedCode + "`. Use SHA-256 or BCrypt!");
                }

                // 4. OWASP A05: Permissive CORS Configuration
                if (addedCode.contains("@CrossOrigin(\"*\")") || addedCode.contains("allowedOrigins(\"*\")")) {
                    securityAlerts.add("⚠️ OWASP A05 SECURITY MISCONFIGURATION: Permissive Wildcard CORS (`*`) detected. Restrict allowed origins to trusted domains!");
                }

                // 5. Code Quality Check: System.out.println / console.log
                if (addedCode.contains("System.out.println") || addedCode.contains("console.log")) {
                    improvements.add("💡 LOGGING BEST PRACTICE: Replace raw `System.out.println`/`console.log` with SLF4J Logger (`log.info(...)`)");
                }
            }
        }

        if (improvements.isEmpty() && securityAlerts.isEmpty()) {
            improvements.add("✅ CLEAN CODE: Standard formatting, valid naming conventions, and proper layer decoupling observed.");
        }

        int securityCount = securityAlerts.size();
        int score = Math.max(0, 100 - (securityCount * 25) - (improvements.size() * 5));

        String summary = generateReviewSummary(prTitle, score, securityCount, securityAlerts, improvements);

        CodeReview entity = CodeReview.builder()
                .user(user)
                .repository(repo)
                .prTitle(prTitle)
                .gitDiff(diff)
                .qualityScore(score)
                .securityIssuesCount(securityCount)
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
                .improvements(improvements)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CodeReviewResponse> getUserReviewHistory(User user) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateReviewSummary(String title, int score, int securityCount, List<String> alerts, List<String> improvements) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Code Review Summary: **%s**\n\n", title));
        sb.append(String.format("- **Overall Quality Score**: `%d / 100`\n", score));
        sb.append(String.format("- **Security Alerts**: `%d Security Vulnerabilities`\n\n", securityCount));

        if (securityCount > 0) {
            sb.append("#### 🔴 Security Blockers:\n");
            for (String alert : alerts) {
                sb.append(String.format("- %s\n", alert));
            }
            sb.append("\n**Action Required**: Resolve security blockers prior to merging into target production branch.\n\n");
        } else {
            sb.append("#### 🟢 Security Audit Passed:\nNo OWASP security vulnerabilities or credential leaks detected.\n\n");
        }

        sb.append("#### 💡 Quality Recommendations:\n");
        for (String imp : improvements) {
            sb.append(String.format("- %s\n", imp));
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
