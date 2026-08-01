import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CodeRepository } from '../../../core/models/repository.model';
import { RepositoryService } from '../../../core/services/repository.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-repo-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './repo-list.component.html',
  styleUrls: ['./repo-list.component.scss']
})
export class RepoListComponent implements OnInit, OnDestroy {
  private repoService = inject(RepositoryService);
  private snackBar = inject(MatSnackBar);
  private pollSub?: Subscription;

  repositories: CodeRepository[] = [];
  loading = true;

  ngOnInit(): void {
    this.loadRepositories(true);
    // Poll status every 3 seconds for repos in CLONING / EXTRACTING state
    this.pollSub = interval(3000).subscribe(() => {
      if (this.repositories.some(r => r.status === 'CLONING' || r.status === 'EXTRACTING')) {
        this.loadRepositories(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  loadRepositories(showSpinner = false): void {
    if (showSpinner) this.loading = true;
    this.repoService.getRepositories().subscribe({
      next: (data) => {
        this.repositories = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  deleteRepo(repo: CodeRepository): void {
    if (confirm(`Are you sure you want to delete repository '${repo.name}'?`)) {
      this.repoService.deleteRepository(repo.uuid).subscribe({
        next: () => {
          this.snackBar.open(`Repository '${repo.name}' deleted`, 'Close', { duration: 3000 });
          this.loadRepositories(false);
        },
        error: () => {
          this.snackBar.open('Failed to delete repository', 'Close', { duration: 3000 });
        }
      });
    }
  }

  formatSize(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
