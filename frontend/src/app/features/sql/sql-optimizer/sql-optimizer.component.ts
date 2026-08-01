import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SqlQueryResponse } from '../../../core/models/sql.model';
import { SqlService } from '../../../core/services/sql.service';

@Component({
  selector: 'app-sql-optimizer',
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
  templateUrl: './sql-optimizer.component.html',
  styleUrls: ['./sql-optimizer.component.scss']
})
export class SqlOptimizerComponent implements OnInit {
  private sqlService = inject(SqlService);
  private snackBar = inject(MatSnackBar);

  sqlInput: string = '';
  optimizationResult: SqlQueryResponse | null = null;
  history: SqlQueryResponse[] = [];
  loading = false;

  sampleQueries = [
    {
      label: 'Slow Unindexed SELECT * Join Query',
      query: `SELECT * FROM users u 
JOIN orders o ON u.id = o.user_id 
WHERE u.status = 'ACTIVE' AND u.email LIKE '%gmail.com'`
    },
    {
      label: 'Full Table Scan Search Query',
      query: `SELECT * FROM code_chunks c 
WHERE c.content LIKE '%Authentication%' 
ORDER BY c.created_at DESC`
    }
  ];

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.sqlService.getHistory().subscribe({
      next: (data) => {
        this.history = data;
      }
    });
  }

  loadPreset(queryText: string): void {
    this.sqlInput = queryText;
  }

  optimize(): void {
    if (!this.sqlInput || this.sqlInput.trim() === '') {
      this.snackBar.open('Please paste a SQL query to optimize', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.sqlService.optimizeQuery(this.sqlInput.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.optimizationResult = res;
        this.loadHistory();
        this.snackBar.open(`SQL optimization complete! Estimated +${res.performanceGainPct}% Gain`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('SQL optimization failed', 'Close', { duration: 3000 });
      }
    });
  }
}
