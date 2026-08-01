package com.codepilot.controller;

import com.codepilot.dto.CodeReviewRequest;
import com.codepilot.dto.CodeReviewResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.CodeReviewerEngine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class CodeReviewerController {

    private final CodeReviewerEngine reviewerEngine;
    private final UserRepository userRepository;

    public CodeReviewerController(CodeReviewerEngine reviewerEngine, UserRepository userRepository) {
        this.reviewerEngine = reviewerEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<CodeReviewResponse> reviewDiff(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CodeReviewRequest request) {
        User user = getUser(userDetails);
        CodeReviewResponse response = reviewerEngine.reviewDiff(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CodeReviewResponse>> getUserReviewHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<CodeReviewResponse> history = reviewerEngine.getUserReviewHistory(user);
        return ResponseEntity.ok(history);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
