package com.codepilot.controller;

import com.codepilot.dto.AnalyticsMetricsDTO;
import com.codepilot.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsMetricsDTO> getDashboardAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        AnalyticsMetricsDTO metrics = analyticsService.getUserAnalyticsByUsername(username);
        return ResponseEntity.ok(metrics);
    }
}
