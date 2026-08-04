import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
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
    MatProgressBarModule,
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
  loadingStage: string = '';
  activeTab: 'ALL' | 'ROOT_CAUSE' | 'FIX_CODE' | 'CHECKLIST' | 'TIMELINE' = 'ALL';

  sampleTraces = [
    {
      label: '🚨 NullPointerException (Unsatisfied DI)',
      trace: `java.lang.NullPointerException: Cannot invoke "com.codepilot.repository.UserRepository.findByUsername(String)" because "this.userRepository" is null
\tat com.codepilot.service.UserService.getUserByEmail(UserService.java:42)
\tat com.codepilot.controller.UserController.getProfile(UserController.java:28)`
    },
    {
      label: '🚨 Spring Boot BeanCreationException',
      trace: `org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'userService': Unsatisfied dependency expressed through constructor parameter 0; nested exception is org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.codepilot.repository.UserRepository' available
\tat org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:798)
\tat org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:229)`
    },
    {
      label: '🔑 ExpiredJwtException',
      trace: `io.jsonwebtoken.ExpiredJwtException: JWT expired 1800000ms ago at 2026-08-01T12:00:00Z. Current time: 2026-08-01T12:30:00Z
\tat io.jsonwebtoken.impl.DefaultJwtParser.parseClaimsJws(DefaultJwtParser.java:565)
\tat com.codepilot.security.JwtTokenProvider.validateToken(JwtTokenProvider.java:62)
\tat com.codepilot.security.JwtAuthenticationFilter.doFilterInternal(JwtAuthenticationFilter.java:38)`
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
      this.snackBar.open('Please paste a stack trace or code snippet to analyze', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.loadingStage = 'Parsing Stack Frames & Deep Root Cause Analysis...';

    setTimeout(() => { this.loadingStage = 'Evaluating Evidence & Probable Cause Matrix...'; }, 200);
    setTimeout(() => { this.loadingStage = 'Generating Production-Grade Fixed Code...'; }, 400);
    setTimeout(() => { this.loadingStage = 'Finalizing Debugging Report & Checklist...'; }, 600);

    this.debugService.analyzeStackTrace(this.stackTraceInput.trim()).subscribe({
      next: (response) => {
        this.loading = false;
        this.analysisResult = response;
        this.loadHistory();
        this.snackBar.open('Stack trace analysis complete!', 'Close', { duration: 3000 });

        setTimeout(() => {
          const el = document.getElementById('debug-results-container');
          if (el) {
            el.scrollIntoView({ behavior: 'smooth' });
          }
        }, 100);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || 'Analysis failed. Check stack trace formatting.';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }

  setActiveTab(tab: 'ALL' | 'ROOT_CAUSE' | 'FIX_CODE' | 'CHECKLIST' | 'TIMELINE'): void {
    this.activeTab = tab;
  }

  copyStackTrace(): void {
    if (!this.stackTraceInput) return;
    navigator.clipboard.writeText(this.stackTraceInput);
    this.snackBar.open('Raw stack trace copied to clipboard!', 'Close', { duration: 2500 });
  }

  copyDiagnosis(): void {
    if (!this.analysisResult) return;
    navigator.clipboard.writeText(this.analysisResult.rootCauseSummary || this.analysisResult.rootCause);
    this.snackBar.open('Diagnosis summary copied!', 'Close', { duration: 2500 });
  }

  copyFixCode(): void {
    if (!this.analysisResult) return;
    navigator.clipboard.writeText(this.analysisResult.fixedCodeExample || this.analysisResult.suggestedFix);
    this.snackBar.open('Fixed code snippet copied!', 'Close', { duration: 2500 });
  }

  copyMarkdownReport(): void {
    if (!this.analysisResult) return;
    navigator.clipboard.writeText(this.analysisResult.fullReportMarkdown || this.analysisResult.rootCause);
    this.snackBar.open('Full Markdown Debug Report copied!', 'Close', { duration: 2500 });
  }

  exportMarkdown(): void {
    if (!this.analysisResult) return;
    const blob = new Blob([this.analysisResult.fullReportMarkdown || this.analysisResult.rootCause], { type: 'text/markdown' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `DebugReport_${this.analysisResult.uuid || 'Report'}.md`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  exportJson(): void {
    if (!this.analysisResult) return;
    const jsonStr = JSON.stringify(this.analysisResult, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `DebugReport_${this.analysisResult.uuid || 'Report'}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  reopenAnalysis(item: ExceptionAnalysisResponse): void {
    this.analysisResult = item;
    setTimeout(() => {
      const el = document.getElementById('debug-results-container');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);
  }

  getPossibleCauseKeys(): string[] {
    return this.analysisResult?.possibleCausesMap ? Object.keys(this.analysisResult.possibleCausesMap) : [];
  }
}
