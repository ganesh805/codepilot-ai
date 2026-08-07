package com.codepilot.controller;

import com.codepilot.dto.CodeOptimizerRequest;
import com.codepilot.dto.CodeOptimizerResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.CodeOptimizerEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/optimizer")
public class CodeOptimizerController {

    private final CodeOptimizerEngine optimizerEngine;
    private final UserRepository userRepository;

    public CodeOptimizerController(
            CodeOptimizerEngine optimizerEngine,
            UserRepository userRepository) {
        this.optimizerEngine = optimizerEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<CodeOptimizerResponse> optimizeCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CodeOptimizerRequest request) {

        User user = null;
        if (userDetails != null) {
            user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseGet(() -> userRepository.findByEmail(userDetails.getUsername()).orElse(null));
        }

        CodeOptimizerResponse response = optimizerEngine.optimizeCode(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CodeOptimizerResponse>> getUserHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = null;
        if (userDetails != null) {
            user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseGet(() -> userRepository.findByEmail(userDetails.getUsername()).orElse(null));
        }

        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<CodeOptimizerResponse> history = optimizerEngine.getUserOptimizerHistory(user);
        return ResponseEntity.ok(history);
    }
}
