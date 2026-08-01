import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EmbeddingResponse, EmbeddingStats } from '../../../core/models/embedding.model';
import { EmbeddingService } from '../../../core/services/embedding.service';

@Component({
  selector: 'app-repo-embedding',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule
  ],
  templateUrl: './repo-embedding.component.html',
  styleUrls: ['./repo-embedding.component.scss']
})
export class RepoEmbeddingComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private embeddingService = inject(EmbeddingService);
  private snackBar = inject(MatSnackBar);

  repoUuid: string = '';
  stats: EmbeddingStats | null = null;
  embeddings: EmbeddingResponse[] = [];
  loading = true;
  generating = false;

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
    if (this.repoUuid) {
      this.loadStats();
    }
  }

  loadStats(): void {
    this.loading = true;
    this.embeddingService.getStats(this.repoUuid).subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  triggerEmbeddingGeneration(): void {
    this.generating = true;
    this.embeddingService.generateEmbeddings(this.repoUuid).subscribe({
      next: (result) => {
        this.generating = false;
        this.embeddings = result;
        this.loadStats();
        this.snackBar.open(`Successfully generated & indexed ${result.length} vector embeddings in Qdrant!`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.generating = false;
        const msg = err.error?.message || 'Vector embedding generation failed. Ensure AST chunks exist.';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }
}
