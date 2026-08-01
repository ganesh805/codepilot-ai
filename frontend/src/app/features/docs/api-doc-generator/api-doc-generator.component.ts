import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiDocResponse } from '../../../core/models/doc.model';
import { DocService } from '../../../core/services/doc.service';

@Component({
  selector: 'app-api-doc-generator',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './api-doc-generator.component.html',
  styleUrls: ['./api-doc-generator.component.scss']
})
export class ApiDocGeneratorComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private docService = inject(DocService);
  private snackBar = inject(MatSnackBar);

  repoUuid: string = '';
  docResult: ApiDocResponse | null = null;
  loading = true;
  generating = false;

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
    if (this.repoUuid) {
      this.loadDocs();
    }
  }

  loadDocs(): void {
    this.loading = true;
    this.docService.getDocs(this.repoUuid).subscribe({
      next: (res) => {
        this.docResult = res;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  triggerDocGeneration(): void {
    this.generating = true;
    this.docService.generateDocs(this.repoUuid).subscribe({
      next: (res) => {
        this.generating = false;
        this.docResult = res;
        this.snackBar.open(`REST API documentation generated for ${res.totalEndpoints} endpoints!`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.generating = false;
        this.snackBar.open('Documentation generation failed', 'Close', { duration: 3000 });
      }
    });
  }

  copyMarkdown(): void {
    if (this.docResult?.markdownSpec) {
      navigator.clipboard.writeText(this.docResult.markdownSpec);
      this.snackBar.open('Markdown REST API Specification copied to clipboard!', 'Close', { duration: 3000 });
    }
  }
}
