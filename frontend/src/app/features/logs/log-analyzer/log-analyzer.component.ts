import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LogAnalysisResponse } from '../../../core/models/log.model';
import { LogService } from '../../../core/services/log.service';

@Component({
  selector: 'app-log-analyzer',
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
  templateUrl: './log-analyzer.component.html',
  styleUrls: ['./log-analyzer.component.scss']
})
export class LogAnalyzerComponent implements OnInit {
  private logService = inject(LogService);
  private snackBar = inject(MatSnackBar);

  logTextInput: string = '';
  selectedFile: File | null = null;
  analysisResult: LogAnalysisResponse | null = null;
  history: LogAnalysisResponse[] = [];
  loading = false;

  sampleLogs = [
    {
      label: 'Production Error Spike Log',
      content: `2026-08-01 12:00:01.102 INFO  [codepilot] Starting CodePilotApplication v1.0.0
2026-08-01 12:00:02.450 INFO  [codepilot] HikariPool-1 - Added connection h2-db-0
2026-08-01 12:02:14.882 WARN  [codepilot] Slow DB Query execution detected (450ms)
2026-08-01 12:05:10.120 ERROR [codepilot] HikariPool-1 - Connection is not available, request timed out after 30000ms.
2026-08-01 12:05:10.122 ERROR [codepilot] org.springframework.dao.CannotAcquireLockException: Could not obtain lock
2026-08-01 12:05:15.340 ERROR [codepilot] io.jsonwebtoken.ExpiredJwtException: JWT Token signature validation failed`
    },
    {
      label: 'Zero Error Startup Log',
      content: `2026-08-01 10:00:00.000 INFO  [codepilot] Bootstrapping Spring Data Repositories...
2026-08-01 10:00:01.250 INFO  [codepilot] Flyway Community Edition 10.10.0 by Redgate
2026-08-01 10:00:01.310 INFO  [codepilot] Migrating schema to version "8 - Application Log File Analyzer"
2026-08-01 10:00:02.100 INFO  [codepilot] Started CodePilotApplication in 2.1 seconds`
    }
  ];

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.logService.getHistory().subscribe({
      next: (data) => {
        this.history = data;
      }
    });
  }

  loadPreset(content: string): void {
    this.logTextInput = content;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.uploadAndAnalyzeFile();
    }
  }

  uploadAndAnalyzeFile(): void {
    if (!this.selectedFile) return;

    this.loading = true;
    this.logService.analyzeLogFile(this.selectedFile).subscribe({
      next: (res) => {
        this.loading = false;
        this.analysisResult = res;
        this.loadHistory();
        this.snackBar.open(`Log file '${res.fileName}' analyzed successfully!`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Log file analysis failed', 'Close', { duration: 3000 });
      }
    });
  }

  analyzeText(): void {
    if (!this.logTextInput || this.logTextInput.trim() === '') {
      this.snackBar.open('Please paste log content to analyze', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.logService.analyzeLogText(this.logTextInput.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.analysisResult = res;
        this.loadHistory();
        this.snackBar.open('Log content analyzed successfully!', 'Close', { duration: 3000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Log analysis failed', 'Close', { duration: 3000 });
      }
    });
  }
}
