import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExceptionAnalysisResponse } from '../models/debug.model';

@Injectable({
  providedIn: 'root'
})
export class DebugService {
  private apiUrl = 'http://localhost:8080/api/v1/debug';
  private http = inject(HttpClient);

  public analyzeStackTrace(stackTrace: string, repositoryUuid?: string): Observable<ExceptionAnalysisResponse> {
    return this.http.post<ExceptionAnalysisResponse>(`${this.apiUrl}/analyze`, { stackTrace, repositoryUuid });
  }

  public getHistory(): Observable<ExceptionAnalysisResponse[]> {
    return this.http.get<ExceptionAnalysisResponse[]>(`${this.apiUrl}/history`);
  }
}
