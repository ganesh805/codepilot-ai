package com.codepilot.controller;

import com.codepilot.dto.CodeChunkResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.RepositoryScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/repos")
public class RepositoryScannerController {

    private final RepositoryScannerService scannerService;
    private final UserRepository userRepository;

    public RepositoryScannerController(RepositoryScannerService scannerService, UserRepository userRepository) {
        this.scannerService = scannerService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{uuid}/scan")
    public ResponseEntity<List<CodeChunkResponse>> scanRepository(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        List<CodeChunkResponse> chunks = scannerService.scanAndChunkRepository(user, uuid);
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/{uuid}/chunks")
    public ResponseEntity<List<CodeChunkResponse>> getRepositoryChunks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        List<CodeChunkResponse> chunks = scannerService.getRepositoryChunks(user, uuid);
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/{uuid}/chunks/stats")
    public ResponseEntity<Map<String, Object>> getRepositoryScanStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        Map<String, Object> stats = scannerService.getRepositoryScanStats(user, uuid);
        return ResponseEntity.ok(stats);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
