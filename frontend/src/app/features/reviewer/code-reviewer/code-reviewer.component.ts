import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
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
    MatSnackBarModule
  ],
  templateUrl: './code-reviewer.component.html',
  styleUrls: ['./code-reviewer.component.scss']
})
export class CodeReviewerComponent implements OnInit {
  private reviewService = inject(ReviewService);
  private snackBar = inject(MatSnackBar);

  prTitleInput: string = 'Add JWT Auth & Security Enhancements';
  diffInput: string = '';
  reviewResult: CodeReviewResponse | null = null;
  history: CodeReviewResponse[] = [];
  loading = false;

  sampleDiffs = [
    {
      label: 'SQL Injection & Hardcoded Secret Diff',
      title: 'Vulnerable User Search Endpoint',
      diff: `diff --git a/src/main/java/com/codepilot/service/UserService.java b/src/main/java/com/codepilot/service/UserService.java
--- a/src/main/java/com/codepilot/service/UserService.java
+++ b/src/main/java/com/codepilot/service/UserService.java
@@ -15,4 +15,6 @@
+    private String apiSecret = "sk_live_998877665544332211";
+    
+    public List<User> searchUsers(String query) {
+        System.out.println("Executing raw SQL query for " + query);
+        return stmt.executeQuery("SELECT * FROM users WHERE name = '" + query + "'");
+    }`
    },
    {
      label: 'Clean Security Auth Diff',
      title: 'Clean JWT Auth Filter Implementation',
      diff: `diff --git a/src/main/java/com/codepilot/security/SecurityConfig.java b/src/main/java/com/codepilot/security/SecurityConfig.java
--- a/src/main/java/com/codepilot/security/SecurityConfig.java
+++ b/src/main/java/com/codepilot/security/SecurityConfig.java
@@ -20,3 +20,5 @@
+    @Bean
+    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
+        http.cors(cors -> cors.configure(http))
+            .authorizeHttpRequests(auth -> auth.requestMatchers("/health").permitAll().anyRequest().authenticated());
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
    this.reviewService.reviewDiff(this.diffInput.trim(), this.prTitleInput.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.reviewResult = res;
        this.loadHistory();
        this.snackBar.open('AI Code Review complete!', 'Close', { duration: 3000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Code review failed', 'Close', { duration: 3000 });
      }
    });
  }
}
