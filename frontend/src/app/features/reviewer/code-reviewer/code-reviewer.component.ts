import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CodeReviewResponse } from '../../../core/models/review.model';
import { ReviewService } from '../../../core/services/review.service';

@Component({
  selector: 'app-code-reviewer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule
  ],
  templateUrl: './code-reviewer.component.html',
  styleUrls: ['./code-reviewer.component.scss']
})
export class CodeReviewerComponent implements OnInit {
  private reviewService = inject(ReviewService);
  private snackBar = inject(MatSnackBar);

  prTitleInput: string = 'PR #42: User Registration & Security Updates';
  diffInput: string = '';
  reviewResult: CodeReviewResponse | null = null;
  history: CodeReviewResponse[] = [];
  loading = false;
  loadingStage: string = '';
  activeFilter: 'ALL' | 'SECURITY' | 'QUALITY' | 'PERFORMANCE' | 'RECOMMENDATIONS' = 'ALL';

  sampleDiffs = [
    {
      label: '🚨 OWASP Vulnerable Diff',
      title: 'Vulnerable User Controller & Password Hash',
      diff: `diff --git a/src/main/java/com/example/service/UserService.java b/src/main/java/com/example/service/UserService.java
index 1234567..89abcdef 100644
--- a/src/main/java/com/example/service/UserService.java
+++ b/src/main/java/com/example/service/UserService.java
@@ -10,15 +10,25 @@ public class UserService {

+    private static final String AWS_SECRET_KEY = "AKIAIOSFODNN7EXAMPLE_SECRET_KEY_98765";
+    private static final String JWT_SIGNING_KEY = "my_super_secret_jwt_key_12345";

+    public User findUserByUsername(String username) {
+        String sqlQuery = "SELECT * FROM users WHERE username = '" + username + "'";
+        return jdbcTemplate.queryForObject(sqlQuery, User.class);
+    }

+    public String hashUserPassword(String rawPassword) throws Exception {
+        MessageDigest md = MessageDigest.getInstance("MD5");
+        byte[] hash = md.digest(rawPassword.getBytes());
+        return new String(hash);
+    }`
    },
    {
      label: '🟢 Clean Security Auth Diff',
      title: 'Clean JWT Auth Filter & BCrypt Hashing',
      diff: `diff --git a/src/main/java/com/codepilot/security/SecurityConfig.java b/src/main/java/com/codepilot/security/SecurityConfig.java
--- a/src/main/java/com/codepilot/security/SecurityConfig.java
+++ b/src/main/java/com/codepilot/security/SecurityConfig.java
@@ -20,3 +20,5 @@
+    @Bean
+    public PasswordEncoder passwordEncoder() {
+        return new BCryptPasswordEncoder();
+    }

+    @Bean
+    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
+        http.cors(cors -> cors.configure(http))
+            .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**").permitAll().anyRequest().authenticated());
+        return http.build();
+    }`
    }
  ];

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.reviewService.getHistory().subscribe({
      next: (data) => {
        this.history = data;
      }
    });
  }

  loadPreset(title: string, diffText: string): void {
    this.prTitleInput = title;
    this.diffInput = diffText;
  }

  runReview(): void {
    if (!this.diffInput || this.diffInput.trim() === '') {
      this.snackBar.open('Please paste a Git diff patch to review', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.loadingStage = 'Parsing Diff & Tokenizing Code...';

    setTimeout(() => { this.loadingStage = 'Running OWASP Top 10 Security Audit...'; }, 200);
    setTimeout(() => { this.loadingStage = 'Analyzing Code Quality & Exception Safety...'; }, 400);
    setTimeout(() => { this.loadingStage = 'Evaluating Performance & Loop Optimization...'; }, 600);
    setTimeout(() => { this.loadingStage = 'Finalizing Decision & Report...'; }, 800);

    this.reviewService.reviewDiff(this.diffInput.trim(), this.prTitleInput.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.reviewResult = res;
        this.loadHistory();
        this.snackBar.open('AI Code Review & Security Audit Complete!', 'Close', { duration: 3000 });

        setTimeout(() => {
          const el = document.getElementById('review-results-container');
          if (el) {
            el.scrollIntoView({ behavior: 'smooth' });
          }
        }, 100);
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Code review failed', 'Close', { duration: 3000 });
      }
    });
  }

  setFilter(filter: 'ALL' | 'SECURITY' | 'QUALITY' | 'PERFORMANCE' | 'RECOMMENDATIONS'): void {
    this.activeFilter = filter;
  }

  copyReview(): void {
    if (!this.reviewResult) return;
    navigator.clipboard.writeText(this.reviewResult.summary);
    this.snackBar.open('Review summary copied to clipboard!', 'Close', { duration: 2500 });
  }

  exportMarkdown(): void {
    if (!this.reviewResult) return;
    const blob = new Blob([this.reviewResult.summary], { type: 'text/markdown' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `CodeReview_${this.reviewResult.uuid || 'Report'}.md`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  exportJson(): void {
    if (!this.reviewResult) return;
    const jsonStr = JSON.stringify(this.reviewResult, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `CodeReview_${this.reviewResult.uuid || 'Report'}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  reopenReview(item: CodeReviewResponse): void {
    this.reviewResult = item;
    setTimeout(() => {
      const el = document.getElementById('review-results-container');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);
  }
}
