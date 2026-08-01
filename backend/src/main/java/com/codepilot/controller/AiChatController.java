package com.codepilot.controller;

import com.codepilot.dto.ChatRequest;
import com.codepilot.dto.ChatResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.AiChatRagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/repos")
public class AiChatController {

    private final AiChatRagService chatService;
    private final UserRepository userRepository;

    public AiChatController(AiChatRagService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{uuid}/chat")
    public ResponseEntity<ChatResponse> chatWithCodebase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uuid,
            @Valid @RequestBody ChatRequest request) {
        User user = getUser(userDetails);
        ChatResponse response = chatService.processChatRequest(user, uuid, request);
        return ResponseEntity.ok(response);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
