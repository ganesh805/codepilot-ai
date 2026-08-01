package com.codepilot.controller;

import com.codepilot.dto.SqlQueryRequest;
import com.codepilot.dto.SqlQueryResponse;
import com.codepilot.entity.User;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.SqlQueryOptimizerEngine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sql")
public class SqlQueryOptimizerController {

    private final SqlQueryOptimizerEngine sqlEngine;
    private final UserRepository userRepository;

    public SqlQueryOptimizerController(SqlQueryOptimizerEngine sqlEngine, UserRepository userRepository) {
        this.sqlEngine = sqlEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/optimize")
    public ResponseEntity<SqlQueryResponse> optimizeQuery(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SqlQueryRequest request) {
        User user = getUser(userDetails);
        SqlQueryResponse response = sqlEngine.optimizeQuery(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<SqlQueryResponse>> getUserSqlHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<SqlQueryResponse> history = sqlEngine.getUserSqlHistory(user);
        return ResponseEntity.ok(history);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
