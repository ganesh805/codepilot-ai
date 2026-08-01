package com.codepilot.controller;

import com.codepilot.dto.UserResponse;
import com.codepilot.entity.RoleName;
import com.codepilot.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{uuid}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String uuid,
            @RequestParam RoleName role) {
        UserResponse response = userService.updateUserRole(uuid, role);
        return ResponseEntity.ok(response);
    }
}
