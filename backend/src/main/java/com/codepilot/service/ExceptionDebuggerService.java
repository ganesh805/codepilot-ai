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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExceptionDebuggerService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionDebuggerService.class);

    private static final Pattern EXCEPTION_TYPE_PATTERN = Pattern.compile("([a-zA-Z0-9_.]+(?:Exception|Error|Failure)):?\\s*(.*)");
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
        long startTime = System.currentTimeMillis();
        String input = request.getStackTrace();
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input stack trace or error log cannot be empty");
        }

        CodeRepository repo = null;
        if (request.getRepositoryUuid() != null && !request.getRepositoryUuid().isEmpty()) {
            repo = repoRepository.findByUuidAndUserId(request.getRepositoryUuid(), user.getId()).orElse(null);
        }

        // 1. ALWAYS PARSE DEEPEST "Caused by:" EXCEPTION FOR TRUE ROOT CAUSE
        String deepestTrace = extractDeepestCauseTrace(input);
        String exceptionType = extractExceptionType(deepestTrace);
        String errorMessage = extractErrorMessage(deepestTrace);

        // Strict App Stack Frame (Returns NULL if non-existent in trace)
        StackFrameAppLocation appLoc = findFirstAppStackFrame(deepestTrace);
        if (appLoc == null) {
            appLoc = findFirstAppStackFrame(input); // Check outer trace if deepest has no app frames
        }

        // 2. KNOWLEDGE BASE EVIDENCE-BASED DIAGNOSTIC COMPUTATION
        String severity = computeSeverity(exceptionType, input);
        ConfidenceDiagnosis confidence = computeConfidence(exceptionType, errorMessage, appLoc);
        String fixTime = computeFixTime(exceptionType, severity);
        String impact = computeProductionImpact(exceptionType, errorMessage, severity);
        String mergeRisk = (severity.equals("Critical") || severity.equals("High")) ? "Critical" : "Low";

        List<String> evidenceList = extractEvidence(exceptionType, errorMessage, appLoc, input);
        Map<String, Integer> possibleCauses = computePossibleCauses(exceptionType, errorMessage, appLoc, input);
        List<String> checklist = computeChecklist(exceptionType, errorMessage);
        List<String> technologies = computeTechnologies(exceptionType, input);
        List<String> preventiveRecs = computePreventiveRecommendations(exceptionType);
        List<String> resources = computeLearningResources(exceptionType);

        String rootCauseSummary = buildRootCauseSummary(exceptionType, errorMessage, appLoc, input);
        String recommendedFix = buildRecommendedFix(exceptionType, errorMessage, appLoc, input);
        String fixedCodeExample = buildFixedCodeExample(exceptionType, errorMessage, appLoc, input);

        List<String> timelineSteps = buildEvidenceTimeline(exceptionType, errorMessage, appLoc, input);

        long durationMs = System.currentTimeMillis() - startTime;

        String fullMarkdownReport = buildEnterpriseMarkdownReport(
                exceptionType, errorMessage, severity, confidence.score, confidence.reason, fixTime, impact,
                appLoc, rootCauseSummary, evidenceList, possibleCauses, recommendedFix,
                fixedCodeExample, checklist, technologies, preventiveRecs, resources
        );

        ExceptionAnalysis entity = ExceptionAnalysis.builder()
                .user(user)
                .repository(repo)
                .exceptionType(exceptionType)
                .errorMessage(errorMessage)
                .stackTrace(input)
                .rootCause(rootCauseSummary)
                .suggestedFix(fixedCodeExample)
                .build();

        ExceptionAnalysis saved = analysisRepository.save(entity);

        return ExceptionAnalysisResponse.builder()
                .uuid(saved.getUuid())
                .exceptionType(exceptionType)
                .errorMessage(errorMessage)
                .severity(severity)
                .confidenceScore(confidence.score)
                .confidenceReason(confidence.reason)
                .rootCauseSummary(rootCauseSummary)
                .estimatedFixTime(fixTime)
                .productionImpact(impact)
                .mergeRisk(mergeRisk)
                .rootCauseFile(appLoc != null ? appLoc.file : "N/A")
                .rootCauseClass(appLoc != null ? appLoc.className : "N/A")
                .rootCauseMethod(appLoc != null ? appLoc.methodName : "N/A")
                .rootCauseLineNumber(appLoc != null ? appLoc.lineNumber : null)
                .evidenceList(evidenceList)
                .possibleCausesMap(possibleCauses)
                .businessImpact(impact)
                .recommendedFix(recommendedFix)
                .fixedCodeExample(fixedCodeExample)
                .debugChecklist(checklist)
                .relatedTechnologies(technologies)
                .preventiveRecommendations(preventiveRecs)
                .learningResources(resources)
                .timelineSteps(timelineSteps)
                .analysisDurationMs(durationMs)
                .fullReportMarkdown(fullMarkdownReport)
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

    // --- INTERNAL EXCEPTION KNOWLEDGE BASE & PARSERS ---

    private String extractDeepestCauseTrace(String input) {
        if (input.contains("Caused by:")) {
            String[] parts = input.split("Caused by:");
            return parts[parts.length - 1].trim();
        }
        return input;
    }

    private String extractExceptionType(String input) {
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (input.contains("NoSuchBeanDefinitionException")) return "org.springframework.beans.factory.NoSuchBeanDefinitionException";
        if (input.contains("BeanCreationException")) return "org.springframework.beans.factory.BeanCreationException";
        if (input.contains("SQLNonTransientConnectionException") || input.contains("CommunicationsException")) return "java.sql.SQLNonTransientConnectionException";
        if (input.contains("BadSqlGrammarException")) return "org.springframework.jdbc.BadSqlGrammarException";
        if (input.contains("ConstraintViolationException") || input.contains("DataIntegrityViolationException")) return "jakarta.validation.ConstraintViolationException";
        if (input.contains("NullPointerException")) return "java.lang.NullPointerException";
        if (input.contains("ExpiredJwtException")) return "io.jsonwebtoken.ExpiredJwtException";
        if (input.contains("SignatureException") || input.contains("MalformedJwtException")) return "io.jsonwebtoken.security.SignatureException";
        if (input.contains("OutOfMemoryError")) return "java.lang.OutOfMemoryError";
        if (input.contains("StackOverflowError")) return "java.lang.StackOverflowError";
        if (input.contains("FileNotFoundException") || input.contains("NoSuchFileException")) return "java.io.FileNotFoundException";
        return "java.lang.RuntimeException";
    }

    private String extractErrorMessage(String input) {
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(input);
        if (matcher.find() && matcher.groupCount() >= 2 && !matcher.group(2).isEmpty()) {
            return matcher.group(2).trim();
        }
        String[] lines = input.split("\n");
        return lines.length > 0 ? lines[0].trim() : "Execution exception analyzed";
    }

    private StackFrameAppLocation findFirstAppStackFrame(String trace) {
        Matcher matcher = STACK_FRAME_PATTERN.matcher(trace);
        while (matcher.find()) {
            String className = matcher.group(1);
            String method = matcher.group(2);
            String file = matcher.group(3);
            int line = Integer.parseInt(matcher.group(4));

            // Skip framework packages
            if (!className.startsWith("org.springframework") && !className.startsWith("java.") 
                    && !className.startsWith("jdk.") && !className.startsWith("org.hibernate") 
                    && !className.startsWith("org.apache") && !className.startsWith("com.sun")
                    && !className.startsWith("io.netty") && !className.startsWith("com.mysql")) {
                return new StackFrameAppLocation(className, method, file, line);
            }
        }
        return null; // Return NULL instead of guessing!
    }

    private String computeSeverity(String type, String input) {
        String upper = (type + " " + input).toUpperCase();
        if (upper.contains("BEANCREATIONEXCEPTION") || upper.contains("NOSUCHBEANDEFINITIONEXCEPTION") 
                || upper.contains("SQLNONTRANSIENTCONNECTIONEXCEPTION") || upper.contains("OUTOFMEMORYERROR") 
                || upper.contains("STACKOVERFLOWERROR") || upper.contains("CONNECTION REFUSED") || upper.contains("ACCESS DENIED")) {
            return "Critical";
        }
        if (upper.contains("EXPIREDJWTEXCEPTION") || upper.contains("SIGNATUREEXCEPTION") 
                || upper.contains("BADCREDENTIALSEXCEPTION") || upper.contains("SQLEXCEPTION") || upper.contains("BADSQLGRAMMAREXCEPTION")) {
            return "High";
        }
        if (upper.contains("NULLPOINTEREXCEPTION") || upper.contains("ILLEGALARGUMENTEXCEPTION") 
                || upper.contains("CONSTRAINTVIOLATIONEXCEPTION") || upper.contains("FILENOTFOUNDEXCEPTION")) {
            return "Medium";
        }
        return "Low";
    }

    private ConfidenceDiagnosis computeConfidence(String type, String msg, StackFrameAppLocation appLoc) {
        String msgLower = msg.toLowerCase();
        if (msgLower.contains("access denied") || msgLower.contains("connection refused") 
                || type.contains("ExpiredJwtException") || type.contains("NoSuchBeanDefinitionException")
                || type.contains("SignatureException") || type.contains("OutOfMemoryError")) {
            return new ConfidenceDiagnosis(100, "Exact exception type and error message signature extracted directly from exception payload.");
        }
        if (appLoc != null) {
            return new ConfidenceDiagnosis(95, String.format("Application stack frame '%s:%d' identified in trace.", appLoc.file, appLoc.lineNumber));
        }
        return new ConfidenceDiagnosis(80, "Framework stack trace analyzed; no application-level stack frame present.");
    }

    private String computeFixTime(String type, String severity) {
        if (severity.equals("Critical")) return "10 - 15 mins";
        if (type.contains("NullPointerException")) return "5 - 10 mins";
        return "10 - 20 mins";
    }

    private String computeProductionImpact(String type, String msg, String severity) {
        if (type.contains("NoSuchBeanDefinitionException") || type.contains("BeanCreationException")) {
            return "Application Startup Failure! Spring IoC Container failed to initialize required beans.";
        }
        if (type.contains("SQLNonTransientConnectionException") || msg.toLowerCase().contains("access denied")) {
            return "Database Infrastructure Unavailable! Database connection or authentication failed.";
        }
        if (type.contains("BadSqlGrammarException")) {
            return "Database Query Failure! Invalid SQL statement executed, resulting in transaction rollback.";
        }
        if (type.contains("NullPointerException")) {
            return "API HTTP 500 Server Error served to users during request execution.";
        }
        if (type.contains("ExpiredJwtException") || type.contains("SignatureException")) {
            return "Authentication Failure! Client JWT Bearer token rejected.";
        }
        if (type.contains("OutOfMemoryError")) {
            return "JVM Crash / OutOfMemoryError! Java Heap Memory exhausted.";
        }
        if (type.contains("StackOverflowError")) {
            return "Thread Crash / StackOverflowError! Infinite recursion exceeded thread call stack size.";
        }
        return "Operational Exception or Business Processing Error.";
    }

    private List<String> extractEvidence(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        List<String> list = new ArrayList<>();
        list.add(String.format("Raw Exception Type: `%s`", type));
        if (msg != null && !msg.isEmpty()) {
            list.add(String.format("Exception Message: `%s`", msg));
        }
        if (appLoc != null) {
            list.add(String.format("Application Stack Frame: `%s.%s()` in `%s` at line %d", appLoc.className, appLoc.methodName, appLoc.file, appLoc.lineNumber));
        } else {
            list.add("Application Stack Frame: N/A (Pure framework execution boundary)");
        }
        return list;
    }

    // --- KNOWLEDGE BASE: POSSIBLE CAUSES ---
    private Map<String, Integer> computePossibleCauses(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String msgLower = msg.toLowerCase();

        if (type.contains("NoSuchBeanDefinitionException")) {
            if (msg.contains("PasswordEncoder")) {
                map.put("Missing PasswordEncoder @Bean definition in SecurityConfig or AppConfig", 100);
            } else {
                map.put("Missing Spring @Service, @Repository, or @Component annotation on target class", 95);
                map.put("Spring @ComponentScan boundary excluding target package", 85);
            }
        } else if (type.contains("SQLNonTransientConnectionException") || msgLower.contains("access denied") || msgLower.contains("connection refused")) {
            map.put("Incorrect database username or password in application.properties / application.yml", 100);
            map.put("Database server offline, container stopped, or firewall blocking port", 90);
        } else if (type.contains("BadSqlGrammarException")) {
            map.put("SQL Syntax Error in query string or column/table name mismatch", 100);
        } else if (type.contains("ConstraintViolationException")) {
            map.put("Database unique constraint, foreign key, or non-null column constraint violation", 100);
        } else if (type.contains("NullPointerException")) {
            if (msg.contains("is null")) {
                map.put(String.format("Null Object Dereference: %s", msg), 100);
            } else {
                map.put("Uninitialized variable or missing Spring constructor injection", 90);
            }
        } else if (type.contains("ExpiredJwtException")) {
            map.put("JWT Bearer token expiration timestamp (exp claim) passed valid lifetime window", 100);
        } else if (type.contains("SignatureException")) {
            map.put("JWT signature secret key mismatch between issuing server and validator", 100);
        } else if (type.contains("OutOfMemoryError")) {
            map.put("Java Heap Memory exhausted due to memory leak or unpaginated large dataset query", 100);
        } else if (type.contains("StackOverflowError")) {
            map.put("Infinite recursive method call without base termination condition", 100);
        } else if (type.contains("FileNotFoundException")) {
            map.put("File does not exist at specified path or application lacks read permissions", 100);
        } else {
            map.put("Unhandled runtime execution boundary failure", 85);
        }
        return map;
    }

    // --- KNOWLEDGE BASE: CHECKLIST ---
    private List<String> computeChecklist(String type, String msg) {
        List<String> list = new ArrayList<>();
        String msgLower = msg.toLowerCase();

        if (type.contains("NoSuchBeanDefinitionException")) {
            if (msg.contains("PasswordEncoder")) {
                list.add("✔ Define `@Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }` in SecurityConfig");
            } else {
                list.add("✔ Verify target class is annotated with `@Service`, `@Repository`, or `@Component`");
                list.add("✔ Check `@ComponentScan` package boundaries in application main class");
            }
        } else if (type.contains("SQLNonTransientConnectionException") || msgLower.contains("access denied") || msgLower.contains("connection refused")) {
            list.add("✔ Verify `spring.datasource.username` and `spring.datasource.password` in application.properties");
            list.add("✔ Confirm database container / service is running on target port");
            list.add("✔ Verify database user permissions on target schema");
        } else if (type.contains("BadSqlGrammarException")) {
            list.add("✔ Inspect SQL query string for syntax errors, reserved keywords, or missing commas");
            list.add("✔ Verify table and column names match database schema");
        } else if (type.contains("ConstraintViolationException")) {
            list.add("✔ Check database table constraints (UNIQUE, FOREIGN KEY, NOT NULL)");
            list.add("✔ Add validation checks before persisting entity");
        } else if (type.contains("NullPointerException")) {
            list.add("✔ Inspect exception line number for uninitialized object reference");
            list.add("✔ Ensure Spring components use Constructor Injection for dependencies");
        } else if (type.contains("ExpiredJwtException")) {
            list.add("✔ Verify JWT token expiration duration setting (`exp` claim)");
            list.add("✔ Implement client-side refresh token flow");
        } else if (type.contains("SignatureException")) {
            list.add("✔ Verify `jwt.secret` configuration in application.properties matches auth server");
        } else if (type.contains("OutOfMemoryError")) {
            list.add("✔ Use pagination (`Pageable`) for large database queries");
            list.add("✔ Analyze JVM Heap Dump using Eclipse MAT or VisualVM");
        } else if (type.contains("StackOverflowError")) {
            list.add("✔ Inspect call stack for repeating recursive method calls");
            list.add("✔ Ensure recursive methods have valid base termination conditions");
        } else if (type.contains("FileNotFoundException")) {
            list.add("✔ Verify target file path exists");
            list.add("✔ Check file read/write permissions for process user");
        } else {
            list.add("✔ Verify application configuration and method parameters");
        }
        return list;
    }

    private List<String> computeTechnologies(String type, String input) {
        List<String> tech = new ArrayList<>();
        tech.add("Java 21");
        if (type.contains("Bean") || type.contains("NoSuchBean")) tech.add("Spring Boot 3.3");
        if (input.contains("sql") || input.contains("jdbc") || type.contains("SQL") || type.contains("BadSql")) tech.add("MySQL / PostgreSQL");
        if (input.contains("jpa") || input.contains("hibernate") || type.contains("ConstraintViolation")) tech.add("Spring Data JPA");
        if (type.contains("Jwt") || type.contains("Signature")) tech.add("Spring Security & JWT");
        return tech;
    }

    private List<String> computePreventiveRecommendations(String type) {
        if (type.contains("SQLNonTransientConnectionException") || type.contains("BadSqlGrammarException")) {
            return Arrays.asList(
                    "Use Flyway or Liquibase database migration scripts to enforce consistent schema DDL.",
                    "Configure HikariCP connection pool health checks (`connection-test-query`)."
            );
        }
        if (type.contains("ExpiredJwtException") || type.contains("SignatureException")) {
            return Arrays.asList(
                    "Implement OAuth2 / JWT Refresh Token flow for seamless token renewal.",
                    "Store JWT secret keys in secure environment variables or vault."
            );
        }
        if (type.contains("OutOfMemoryError") || type.contains("StackOverflowError")) {
            return Arrays.asList(
                    "Set appropriate JVM Heap limits (`-Xmx2g`) and configure GC logging.",
                    "Refactor deep recursion into iterative loops."
            );
        }
        return Arrays.asList(
                "Use Spring Constructor Injection with `final` fields to guarantee non-null bean initialization.",
                "Implement `@RestControllerAdvice` Global Exception Handler for standardized API error responses."
        );
    }

    private List<String> computeLearningResources(String type) {
        if (type.contains("SQL") || type.contains("BadSql")) {
            return Arrays.asList("Spring JDBC & HikariCP Configuration Guide", "MySQL / PostgreSQL SQL Syntax Reference");
        }
        if (type.contains("Jwt") || type.contains("Signature")) {
            return Arrays.asList("Spring Security JWT Authentication Guide", "jjwt Library Documentation");
        }
        return Arrays.asList("Spring Framework Reference: Dependency Injection", "Oracle Java Exception Handling Guide");
    }

    // --- KNOWLEDGE BASE: ROOT CAUSE SUMMARY ---
    private String buildRootCauseSummary(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        if (type.contains("NoSuchBeanDefinitionException")) {
            if (msg.contains("PasswordEncoder")) {
                return "NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.security.crypto.password.PasswordEncoder' available.";
            }
            return String.format("NoSuchBeanDefinitionException: No qualifying bean available for requested type in Spring context (%s).", msg);
        }
        if (type.contains("BeanCreationException")) {
            if (rawInput.contains("Access denied")) {
                return "Database Authentication Failure: Access denied for database user specified in spring.datasource.";
            }
            return String.format("BeanCreationException: Spring failed to create bean due to nested exception: %s", msg);
        }
        if (type.contains("SQLNonTransientConnectionException") || msg.toLowerCase().contains("access denied") || msg.toLowerCase().contains("connection refused")) {
            return String.format("Database Connection Failure: %s", msg);
        }
        if (type.contains("BadSqlGrammarException")) {
            return String.format("BadSqlGrammarException: SQL syntax error in query statement (%s).", msg);
        }
        if (type.contains("ConstraintViolationException")) {
            return String.format("ConstraintViolationException: Database constraint violated (%s).", msg);
        }
        if (type.contains("NullPointerException")) {
            if (msg.contains("is null")) {
                return String.format("NullPointerException: %s", msg);
            }
            return String.format("NullPointerException in %s: Invoking a method on an uninitialized null reference.", 
                    appLoc != null ? appLoc.file + ":" + appLoc.lineNumber : "application code");
        }
        if (type.contains("ExpiredJwtException")) {
            return "ExpiredJwtException: JWT Bearer token timestamp (exp claim) has expired.";
        }
        if (type.contains("SignatureException")) {
            return "SignatureException: JWT signature validation failed due to secret key mismatch or token tampering.";
        }
        if (type.contains("OutOfMemoryError")) {
            return "OutOfMemoryError: Java Heap Memory space exhausted.";
        }
        if (type.contains("StackOverflowError")) {
            return "StackOverflowError: Infinite recursion exceeded thread execution stack size.";
        }
        if (type.contains("FileNotFoundException")) {
            return String.format("FileNotFoundException: Cannot find target file (%s).", msg);
        }
        return String.format("%s: %s", type, msg);
    }

    // --- KNOWLEDGE BASE: RECOMMENDED FIX ---
    private String buildRecommendedFix(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        if (type.contains("NoSuchBeanDefinitionException")) {
            if (msg.contains("PasswordEncoder")) {
                return "Evidence: 'No qualifying bean of type PasswordEncoder'. Remedy: Define a PasswordEncoder @Bean in your SecurityConfig class.";
            }
            return String.format("Evidence: 'No qualifying bean of type %s'. Remedy: Annotate the class with @Component, @Service, or @Repository, or define a @Bean method.", msg);
        }
        if (type.contains("SQLNonTransientConnectionException") || msg.toLowerCase().contains("access denied") || rawInput.contains("Access denied")) {
            return "Evidence: 'Access denied for user'. Remedy: Update `spring.datasource.username` and `spring.datasource.password` in application.properties to match database credentials.";
        }
        if (msg.toLowerCase().contains("connection refused")) {
            return "Evidence: 'Connection refused'. Remedy: Start the database container/service and verify host and port in application.properties.";
        }
        if (type.contains("BadSqlGrammarException")) {
            return "Evidence: SQL Syntax Error. Remedy: Correct the SQL query syntax, column names, or table names in your repository query.";
        }
        if (type.contains("ConstraintViolationException")) {
            return "Evidence: Database constraint violation. Remedy: Add duplicate/validation checks before saving entity to avoid constraint failure.";
        }
        if (type.contains("NullPointerException")) {
            if (msg.contains("is null")) {
                return String.format("Evidence: '%s'. Remedy: Ensure the field is injected via Spring Constructor Injection with 'final' keyword.", msg);
            }
            return "Evidence: Null reference dereference. Remedy: Ensure target class uses Spring Constructor Injection instead of 'new'.";
        }
        if (type.contains("ExpiredJwtException")) {
            return "Evidence: 'JWT expired'. Remedy: Client must initiate refresh token flow to acquire a new valid JWT access token.";
        }
        if (type.contains("SignatureException")) {
            return "Evidence: 'JWT signature does not match'. Remedy: Ensure `jwt.secret` in application.properties matches the secret key used by the issuing server.";
        }
        if (type.contains("OutOfMemoryError")) {
            return "Evidence: 'Java heap space'. Remedy: Implement pagination for queries, avoid loading large datasets into memory, or increase JVM heap (-Xmx).";
        }
        if (type.contains("StackOverflowError")) {
            return "Evidence: Recursive call stack exhaustion. Remedy: Add a base termination condition to stop infinite recursion.";
        }
        if (type.contains("FileNotFoundException")) {
            return "Evidence: File path missing. Remedy: Check file path, file existence, and read permissions.";
        }
        return "Evidence: Exception in stack trace. Remedy: Enclose execution in try-catch handling and log detailed error message.";
    }

    // --- KNOWLEDGE BASE: FIXED CODE EXAMPLES ---
    private String buildFixedCodeExample(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        if (type.contains("NoSuchBeanDefinitionException") && msg.contains("PasswordEncoder")) {
            return """
                   // 🟢 FIXED CODE (SecurityConfig.java):
                   @Configuration
                   @EnableWebSecurity
                   public class SecurityConfig {

                       @Bean
                       public PasswordEncoder passwordEncoder() {
                           return new BCryptPasswordEncoder();
                       }
                   }
                   """;
        }
        if (type.contains("NoSuchBeanDefinitionException") || type.contains("NullPointerException")) {
            return """
                   // 🟢 FIXED CODE (Spring Boot Constructor Injection):
                   @Service
                   public class UserService {

                       private final UserRepository userRepository;

                       public UserService(UserRepository userRepository) {
                           this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
                       }

                       public User findUser(String username) {
                           return userRepository.findByUsername(username)
                                   .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
                       }
                   }
                   """;
        }
        if (type.contains("SQLNonTransientConnectionException") || rawInput.contains("Access denied")) {
            return """
                   # 🟢 FIXED CONFIGURATION (application.properties):
                   spring.datasource.url=jdbc:mysql://localhost:3306/taskmanager_db?useSSL=false&serverTimezone=UTC
                   spring.datasource.username=root
                   spring.datasource.password=secret123
                   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
                   """;
        }
        if (type.contains("BadSqlGrammarException")) {
            return """
                   // 🟢 FIXED REPOSITORY QUERY (UserRepository.java):
                   @Query("SELECT u FROM User u WHERE u.username = :username") // Corrected JPQL Syntax
                   Optional<User> findByUsername(@Param("username") String username);
                   """;
        }
        if (type.contains("ExpiredJwtException") || type.contains("SignatureException")) {
            return """
                   // 🟢 FIXED CODE (JwtAuthenticationFilter.java):
                   try {
                       String token = parseJwt(request);
                       if (token != null && tokenProvider.validateToken(token)) {
                           // Authenticate user session...
                       }
                   } catch (ExpiredJwtException ex) {
                       log.warn("JWT Session Expired: {}", ex.getMessage());
                       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                       response.setContentType("application/json");
                       response.getWriter().write("{\\"error\\": \\"JWT Session Expired\\", \\"code\\": 401}");
                       return;
                   }
                   """;
        }
        if (type.contains("StackOverflowError")) {
            return """
                   // 🟢 FIXED RECURSIVE METHOD:
                   public int calculateFactorial(int n) {
                       // Base termination condition to prevent StackOverflowError
                       if (n <= 1) {
                           return 1;
                       }
                       return n * calculateFactorial(n - 1);
                   }
                   """;
        }
        return """
               // 🟢 FIXED CODE:
               try {
                   // Execute domain operation...
               } catch (Exception ex) {
                   log.error("Operation failed: {}", ex.getMessage(), ex);
                   throw new ServiceOperationException("Operation failed: " + ex.getMessage());
               }
               """;
    }

    private List<String> buildEvidenceTimeline(String type, String msg, StackFrameAppLocation appLoc, String rawInput) {
        List<String> steps = new ArrayList<>();
        steps.add("Exception: " + type);
        if (rawInput.contains("Access denied")) {
            steps.add("JDBC Driver ➔ Access Denied ('root'@'localhost') ➔ Fix Database Credentials");
        } else if (msg.contains("PasswordEncoder")) {
            steps.add("Spring Security Context ➔ Missing PasswordEncoder @Bean ➔ Define BCryptPasswordEncoder");
        } else if (appLoc != null) {
            steps.add("File: " + appLoc.file);
            steps.add("Line " + appLoc.lineNumber + " (" + appLoc.methodName + ")");
            steps.add("Class: " + appLoc.className);
        } else {
            steps.add("Framework Execution Boundary ➔ N/A App Frame ➔ Inspect Exception Message");
        }
        return steps;
    }

    private String buildEnterpriseMarkdownReport(
            String type, String msg, String severity, int confidence, String confReason, String fixTime, String impact,
            StackFrameAppLocation appLoc, String rootCause, List<String> evidence, Map<String, Integer> possibleCauses,
            String fix, String codeExample, List<String> checklist, List<String> tech, List<String> preventiveRecs, List<String> resources) {

        StringBuilder sb = new StringBuilder();
        sb.append("# 🐞 Exception Diagnostic Report\n\n");

        sb.append("## 📊 Executive Summary\n");
        sb.append(String.format("- **Exception Type**: `%s`\n", type));
        sb.append(String.format("- **Severity**: `%s` | **Confidence Score**: `%d%%` (`%s`)\n", severity, confidence, confReason));
        sb.append(String.format("- **Root Cause Location**: `%s` (Line %s)\n", appLoc != null ? appLoc.file : "N/A", appLoc != null ? String.valueOf(appLoc.lineNumber) : "N/A"));
        sb.append(String.format("- **Estimated Fix Time**: `%s`\n", fixTime));
        sb.append(String.format("- **Production Impact**: %s\n\n", impact));

        sb.append("-----------------------------------\n");
        sb.append("## 🔍 Root Cause Analysis\n\n");
        sb.append(String.format("**Root Cause**: %s\n\n", rootCause));

        sb.append("-----------------------------------\n");
        sb.append("## 📌 Evidence & Ranked Causes\n\n");
        for (String ev : evidence) {
            sb.append(String.format("- %s\n", ev));
        }
        sb.append("\n**Probable Causes Matrix**:\n");
        possibleCauses.forEach((cause, pct) -> sb.append(String.format("- **%d%% Probability**: %s\n", pct, cause)));

        sb.append("\n-----------------------------------\n");
        sb.append("## 🛠️ Recommended Fix & Solution\n\n");
        sb.append(String.format("%s\n\n", fix));
        sb.append("```java\n");
        sb.append(codeExample);
        sb.append("\n```\n\n");

        sb.append("-----------------------------------\n");
        sb.append("## 📋 Debugging Checklist\n\n");
        for (String item : checklist) {
            sb.append(String.format("%s\n", item));
        }

        return sb.toString();
    }

    private ExceptionAnalysisResponse mapToResponse(ExceptionAnalysis entity) {
        return ExceptionAnalysisResponse.builder()
                .uuid(entity.getUuid())
                .exceptionType(entity.getExceptionType())
                .errorMessage(entity.getErrorMessage())
                .severity("Medium")
                .confidenceScore(85)
                .confidenceReason("Retrieved from history")
                .rootCauseSummary(entity.getRootCause())
                .estimatedFixTime("10 mins")
                .productionImpact("Handled Runtime Exception")
                .mergeRisk("Low")
                .rootCauseFile("N/A")
                .rootCauseClass("N/A")
                .rootCauseMethod("N/A")
                .rootCauseLineNumber(null)
                .evidenceList(Arrays.asList("Saved Analysis History"))
                .possibleCausesMap(Collections.singletonMap("Runtime Exception", 85))
                .businessImpact("Handled runtime exception")
                .recommendedFix(entity.getSuggestedFix())
                .fixedCodeExample(entity.getSuggestedFix())
                .debugChecklist(Arrays.asList("✔ Check exception logs"))
                .relatedTechnologies(Arrays.asList("Java 21", "Spring Boot 3.3"))
                .preventiveRecommendations(Arrays.asList("Add null checks"))
                .learningResources(Arrays.asList("Spring Boot Docs"))
                .timelineSteps(Arrays.asList("Exception Analyzed"))
                .analysisDurationMs(150)
                .fullReportMarkdown(entity.getRootCause())
                .matchedCitations(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static class ConfidenceDiagnosis {
        final int score;
        final String reason;

        ConfidenceDiagnosis(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    private static class StackFrameAppLocation {
        final String className;
        final String methodName;
        final String file;
        final int lineNumber;

        StackFrameAppLocation(String className, String methodName, String file, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.file = file;
            this.lineNumber = lineNumber;
        }
    }
}
