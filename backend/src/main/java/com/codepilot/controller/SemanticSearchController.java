package com.codepilot.controller;

import com.codepilot.dto.SearchRequest;
import com.codepilot.dto.SearchResultDTO;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/repos")
public class SemanticSearchController {

    private final SemanticSearchService searchService;
    private final UserRepository userRepository;

    public SemanticSearchController(SemanticSearchService searchService, UserRepository userRepository) {
        this.searchService = searchService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{uuid}/search")
    public ResponseEntity<List<SearchResultDTO>> searchCodebase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid,
            @Valid @RequestBody SearchRequest request) {
        User user = getUser(userDetails);
        List<SearchResultDTO> results = searchService.searchCodebase(user, uuid, request);
        return ResponseEntity.ok(results);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
