import { Component, OnInit, inject } from '@angular/core';
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
export class RepoListComponent implements OnInit {
  private repoService = inject(RepositoryService);
  private snackBar = inject(MatSnackBar);

  repositories: CodeRepository[] = [];
  loading = true;

  ngOnInit(): void {
    this.loadRepositories();
  }

  loadRepositories(): void {
    this.loading = true;
    this.repoService.getRepositories().subscribe({
      next: (data) => {
        this.repositories = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open('Failed to load repositories', 'Close', { duration: 3000 });
      }
    });
  }

  deleteRepo(repo: CodeRepository): void {
    if (confirm(`Are you sure you want to delete repository '${repo.name}'?`)) {
      this.repoService.deleteRepository(repo.uuid).subscribe({
        next: () => {
          this.snackBar.open(`Repository '${repo.name}' deleted`, 'Close', { duration: 3000 });
          this.loadRepositories();
        },
        error: () => {
          this.snackBar.open('Failed to delete repository', 'Close', { duration: 3000 });
        }
      });
    }
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
