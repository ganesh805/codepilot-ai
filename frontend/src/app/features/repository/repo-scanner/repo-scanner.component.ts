import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
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
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatInputModule,
    MatFormFieldModule,
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
  filteredChunks: CodeChunk[] = [];
  pagedChunks: CodeChunk[] = [];
  stats: ScanStats | null = null;

  searchQuery: string = '';
  selectedLanguage: string = 'ALL';

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
        this.chunks = chunks.map(c => ({
          ...c,
          content: this.cleanContent(c.content)
        }));
        this.applyFilters();
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
        this.chunks = chunks.map(c => ({
          ...c,
          content: this.cleanContent(c.content)
        }));
        this.applyFilters();
        this.loadStats();
        this.snackBar.open(`Repository scanned successfully! ${chunks.length} chunks generated.`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.scanning = false;
        this.snackBar.open('Scanning failed. Check repository file tree.', 'Close', { duration: 4000 });
      }
    });
  }

  cleanContent(content: string): string {
    if (!content) return '';
    let text = content;
    text = text.replace(/^(?:\/\/\s*Context:[^\n]*\n*|\/\*\s*Context Metadata:[\s\S]*?\*\/\s*)/g, '');
    if (text.startsWith('// Context:')) {
      const idx = text.indexOf('\n');
      if (idx !== -1) {
        text = text.substring(idx + 1);
      } else {
        const pkg = text.indexOf('package ');
        const imp = text.indexOf('import ');
        const pub = text.indexOf('public ');
        let min = -1;
        if (pkg !== -1) min = pkg;
        if (imp !== -1 && (min === -1 || imp < min)) min = imp;
        if (pub !== -1 && (min === -1 || pub < min)) min = pub;
        if (min !== -1) text = text.substring(min);
      }
    }
    return text.trim();
  }

  filterByLanguage(lang: string): void {
    this.selectedLanguage = lang;
    this.pageIndex = 0;
    this.applyFilters();
  }

  onSearchChange(): void {
    this.pageIndex = 0;
    this.applyFilters();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.onSearchChange();
  }

  private applyFilters(): void {
    let result = [...this.chunks];

    if (this.selectedLanguage !== 'ALL') {
      result = result.filter(c => c.language.toLowerCase() === this.selectedLanguage.toLowerCase());
    }

    if (this.searchQuery && this.searchQuery.trim() !== '') {
      const q = this.searchQuery.trim().toLowerCase();
      result = result.filter(c => 
        c.fileName.toLowerCase().includes(q) || 
        c.filePath.toLowerCase().includes(q) ||
        c.content.toLowerCase().includes(q)
      );
    }

    this.filteredChunks = result;
    this.updatePagedChunks();
  }

  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    this.updatePagedChunks();
  }

  private updatePagedChunks(): void {
    const startIndex = this.pageIndex * this.pageSize;
    this.pagedChunks = this.filteredChunks.slice(startIndex, startIndex + this.pageSize);
  }

  getLanguageKeys(): string[] {
    return this.stats?.languageBreakdown ? Object.keys(this.stats.languageBreakdown) : [];
  }
}
