import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExceptionAnalysisResponse } from '../../../core/models/debug.model';
import { DebugService } from '../../../core/services/debug.service';

@Component({
  selector: 'app-exception-analyzer',
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
  templateUrl: './exception-analyzer.component.html',
  styleUrls: ['./exception-analyzer.component.scss']
})
export class ExceptionAnalyzerComponent implements OnInit {
  private debugService = inject(DebugService);
  private snackBar = inject(MatSnackBar);

  stackTraceInput: string = '';
  analysisResult: ExceptionAnalysisResponse | null = null;
  history: ExceptionAnalysisResponse[] = [];
  loading = false;

  sampleTraces = [
    {
      label: 'NullPointerException',
      trace: `java.lang.NullPointerException: Cannot invoke "com.codepilot.entity.User.getRoles()" because "user" is null
\tat com.codepilot.service.UserService.getUserRoles(UserService.java:45)
\tat com.codepilot.controller.AdminController.getUsers(AdminController.java:28)`
    },
    {
      label: 'ExpiredJwtException',
      trace: `io.jsonwebtoken.ExpiredJwtException: JWT expired 1800000ms ago at 2026-08-01T12:00:00Z. Current time: 2026-08-01T12:30:00Z
\tat io.jsonwebtoken.impl.DefaultJwtParser.parseClaimsJws(DefaultJwtParser.java:565)
\tat com.codepilot.security.JwtTokenProvider.validateToken(JwtTokenProvider.java:62)
\tat com.codepilot.security.JwtAuthenticationFilter.doFilterInternal(JwtAuthenticationFilter.java:38)`
    },
    {
      label: 'ZipSlip Security Traversal',
      trace: `java.lang.SecurityException: Bad zip entry (Zip Slip vulnerability attempt): ../../etc/passwd
\tat com.codepilot.service.RepositoryImportService.zipSlipProtect(RepositoryImportService.java:184)
\tat com.codepilot.service.RepositoryImportService.unzipSafely(RepositoryImportService.java:162)`
    }
  ];

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.debugService.getHistory().subscribe({
      next: (data) => {
        this.history = data;
      }
    });
  }

  loadPreset(traceText: string): void {
    this.stackTraceInput = traceText;
  }

  analyzeTrace(): void {
    if (!this.stackTraceInput || this.stackTraceInput.trim() === '') {
      this.snackBar.open('Please paste a stack trace to analyze', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.debugService.analyzeStackTrace(this.stackTraceInput.trim()).subscribe({
      next: (response) => {
        this.loading = false;
        this.analysisResult = response;
        this.loadHistory();
        this.snackBar.open('Stack trace analysis complete!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || 'Analysis failed. Check stack trace formatting.';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }
}
