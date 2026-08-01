import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CodeReviewResponse } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiUrl = 'http://localhost:8080/api/v1/reviews';
  private http = inject(HttpClient);

  public reviewDiff(gitDiff: string, prTitle = 'PR Review', repositoryUuid?: string): Observable<CodeReviewResponse> {
    return this.http.post<CodeReviewResponse>(`${this.apiUrl}/analyze`, { gitDiff, prTitle, repositoryUuid });
  }

  public getHistory(): Observable<CodeReviewResponse[]> {
    return this.http.get<CodeReviewResponse[]>(`${this.apiUrl}/history`);
  }
}
