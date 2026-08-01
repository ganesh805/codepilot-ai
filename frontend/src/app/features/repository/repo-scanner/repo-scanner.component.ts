import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { CodeChunk, ScanStats } from '../../../core/models/chunk.model';
import { ScannerService } from '../../../core/services/scanner.service';

@Component({
  selector: 'app-repo-scanner',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatPaginatorModule
  ],
  templateUrl: './repo-scanner.component.html',
  styleUrls: ['./repo-scanner.component.scss']
})
export class RepoScannerComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private scannerService = inject(ScannerService);
  private snackBar = inject(MatSnackBar);

  repoUuid: string = '';
  chunks: CodeChunk[] = [];
  pagedChunks: CodeChunk[] = [];
  stats: ScanStats | null = null;
  loading = true;
  scanning = false;

  pageSize = 10;
  pageIndex = 0;

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
    if (this.repoUuid) {
      this.loadData();
    }
  }

  loadData(): void {
    this.loading = true;
    this.scannerService.getChunks(this.repoUuid).subscribe({
      next: (chunks) => {
        this.chunks = chunks;
        this.updatePagedChunks();
        this.loadStats();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.scannerService.getStats(this.repoUuid).subscribe({
      next: (statsData) => {
        this.stats = statsData;
      }
    });
  }

  triggerScan(): void {
    this.scanning = true;
    this.scannerService.scanRepository(this.repoUuid).subscribe({
      next: (chunks) => {
        this.scanning = false;
        this.chunks = chunks;
        this.pageIndex = 0;
        this.updatePagedChunks();
        this.loadStats();
        this.snackBar.open(`Repository scanned successfully! ${chunks.length} chunks generated.`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.scanning = false;
        this.snackBar.open('Scanning failed. Check repository file tree.', 'Close', { duration: 4000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    this.updatePagedChunks();
  }

  private updatePagedChunks(): void {
    const startIndex = this.pageIndex * this.pageSize;
    this.pagedChunks = this.chunks.slice(startIndex, startIndex + this.pageSize);
  }

  getLanguageKeys(): string[] {
    return this.stats?.languageBreakdown ? Object.keys(this.stats.languageBreakdown) : [];
  }
}
