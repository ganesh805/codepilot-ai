package com.codepilot.controller;

import com.codepilot.dto.ExceptionAnalysisRequest;
import com.codepilot.dto.ExceptionAnalysisResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.ExceptionDebuggerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/debug")
public class ExceptionDebuggerController {

    private final ExceptionDebuggerService debuggerService;
    private final UserRepository userRepository;

    public ExceptionDebuggerController(ExceptionDebuggerService debuggerService, UserRepository userRepository) {
        this.debuggerService = debuggerService;
        this.userRepository = userRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ExceptionAnalysisResponse> analyzeStackTrace(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExceptionAnalysisRequest request) {
        User user = getUser(userDetails);
        ExceptionAnalysisResponse response = debuggerService.analyzeStackTrace(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ExceptionAnalysisResponse>> getUserHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<ExceptionAnalysisResponse> history = debuggerService.getUserAnalysisHistory(user);
        return ResponseEntity.ok(history);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
