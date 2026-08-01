package com.codepilot.controller;

import com.codepilot.dto.LogAnalysisResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.LogAnalyzerEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/logs")
public class LogAnalyzerController {

    private final LogAnalyzerEngine logEngine;
    private final UserRepository userRepository;

    public LogAnalyzerController(LogAnalyzerEngine logEngine, UserRepository userRepository) {
        this.logEngine = logEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/analyze/text")
    public ResponseEntity<LogAnalysisResponse> analyzeLogText(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {
        User user = getUser(userDetails);
        String fileName = payload.getOrDefault("fileName", "pasted-log.txt");
        String content = payload.get("content");
        LogAnalysisResponse response = logEngine.analyzeLogContent(user, fileName, content);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze/upload")
    public ResponseEntity<LogAnalysisResponse> analyzeLogFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        User user = getUser(userDetails);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            LogAnalysisResponse response = logEngine.analyzeLogContent(user, file.getOriginalFilename(), content);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read uploaded log file: " + ex.getMessage(), ex);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<LogAnalysisResponse>> getLogHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<LogAnalysisResponse> history = logEngine.getUserLogHistory(user);
        return ResponseEntity.ok(history);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
