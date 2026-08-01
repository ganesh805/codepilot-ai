package com.codepilot.controller;

import com.codepilot.dto.CodeRepositoryResponse;
import com.codepilot.dto.GitImportRequest;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.RepositoryImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/repos")
public class RepositoryImportController {

    private final RepositoryImportService repoService;
    private final UserRepository userRepository;

    public RepositoryImportController(RepositoryImportService repoService, UserRepository userRepository) {
        this.repoService = repoService;
        this.userRepository = userRepository;
    }

    @PostMapping("/import/github")
    public ResponseEntity<CodeRepositoryResponse> importGithubRepo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GitImportRequest request) {
        User user = getUser(userDetails);
        CodeRepositoryResponse response = repoService.importFromGithub(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/import/zip")
    public ResponseEntity<CodeRepositoryResponse> importZipRepo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        User user = getUser(userDetails);
        CodeRepositoryResponse response = repoService.importFromZip(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CodeRepositoryResponse>> getUserRepositories(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(repoService.getUserRepositories(user));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CodeRepositoryResponse> getRepositoryByUuid(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(repoService.getRepositoryByUuid(user, uuid));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteRepository(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        repoService.deleteRepository(user, uuid);
        return ResponseEntity.noContent().build();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
