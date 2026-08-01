package com.codepilot.controller;

import com.codepilot.dto.EmbeddingResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/repos")
public class EmbeddingController {

    private final EmbeddingService embeddingService;
    private final UserRepository userRepository;

    public EmbeddingController(EmbeddingService embeddingService, UserRepository userRepository) {
        this.embeddingService = embeddingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{uuid}/embeddings/generate")
    public ResponseEntity<List<EmbeddingResponse>> generateEmbeddings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        List<EmbeddingResponse> embeddings = embeddingService.generateAndStoreEmbeddings(user, uuid);
        return ResponseEntity.ok(embeddings);
    }

    @GetMapping("/{uuid}/embeddings/stats")
    public ResponseEntity<Map<String, Object>> getEmbeddingStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        Map<String, Object> stats = embeddingService.getEmbeddingStats(user, uuid);
        return ResponseEntity.ok(stats);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
