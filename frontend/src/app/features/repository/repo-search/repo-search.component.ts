import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SearchResult } from '../../../core/models/search.model';
import { SearchService } from '../../../core/services/search.service';

@Component({
  selector: 'app-repo-search',
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
  templateUrl: './repo-search.component.html',
  styleUrls: ['./repo-search.component.scss']
})
export class RepoSearchComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private searchService = inject(SearchService);
  private snackBar = inject(MatSnackBar);

  repoUuid: string = '';
  searchQuery: string = '';
  results: SearchResult[] = [];
  loading = false;
  hasSearched = false;

  sampleQueries: string[] = [
    'BCrypt password hashing and security configuration',
    'JWT authentication token provider and filter',
    'Repository import git clone and zip extraction',
    'User registration and database entity'
  ];

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
  }

  fillSampleQuery(query: string): void {
    this.searchQuery = query;
    this.executeSearch();
  }

  executeSearch(): void {
    if (!this.searchQuery || this.searchQuery.trim() === '') {
      this.snackBar.open('Please enter a natural language search query', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.hasSearched = true;

    this.searchService.searchCodebase(this.repoUuid, this.searchQuery.trim(), 5).subscribe({
      next: (data) => {
        this.results = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || 'Semantic search failed. Ensure chunks and embeddings exist.';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }

  getScorePercentage(score: number): number {
    return Math.round(score * 100);
  }
}
