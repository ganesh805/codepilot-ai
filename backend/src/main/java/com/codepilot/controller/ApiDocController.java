package com.codepilot.controller;

import com.codepilot.dto.ApiDocResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.ApiDocGeneratorEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/repos")
public class ApiDocController {

    private final ApiDocGeneratorEngine docEngine;
    private final UserRepository userRepository;

    public ApiDocController(ApiDocGeneratorEngine docEngine, UserRepository userRepository) {
        this.docEngine = docEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/{uuid}/docs/generate")
    public ResponseEntity<ApiDocResponse> generateDocs(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        ApiDocResponse response = docEngine.generateApiDocumentation(user, uuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}/docs")
    public ResponseEntity<ApiDocResponse> getDocs(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid) {
        User user = getUser(userDetails);
        ApiDocResponse response = docEngine.getLatestApiDoc(user, uuid);
        return ResponseEntity.ok(response);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
