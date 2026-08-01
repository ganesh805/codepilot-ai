import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { RepositoryService } from '../../../core/services/repository.service';

@Component({
  selector: 'app-repo-import',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './repo-import.component.html',
  styleUrls: ['./repo-import.component.scss']
})
export class RepoImportComponent {
  private fb = inject(FormBuilder);
  private repoService = inject(RepositoryService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  githubForm: FormGroup = this.fb.group({
    gitUrl: ['', [Validators.required, Validators.pattern('https?://.*')]],
    branch: ['main']
  });

  selectedZipFile: File | null = null;
  loading = false;

  onGithubSubmit(): void {
    if (this.githubForm.invalid) return;

    this.loading = true;
    this.repoService.importGithub(this.githubForm.value).subscribe({
      next: (repo) => {
        this.loading = false;
        this.snackBar.open(`Repository '${repo.name}' imported successfully!`, 'Close', { duration: 3000 });
        this.router.navigate(['/repositories']);
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open('GitHub import failed. Ensure public repository URL is valid.', 'Close', { duration: 4000 });
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedZipFile = input.files[0];
    }
  }

  onZipSubmit(): void {
    if (!this.selectedZipFile) return;

    this.loading = true;
    this.repoService.importZip(this.selectedZipFile).subscribe({
      next: (repo) => {
        this.loading = false;
        this.snackBar.open(`ZIP Archive '${repo.name}' extracted successfully!`, 'Close', { duration: 3000 });
        this.router.navigate(['/repositories']);
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open('ZIP extraction failed. Ensure file is a valid .zip archive.', 'Close', { duration: 4000 });
      }
    });
  }
}
