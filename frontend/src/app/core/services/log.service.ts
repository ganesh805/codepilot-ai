import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LogAnalysisResponse } from '../models/log.model';

@Injectable({
  providedIn: 'root'
})
export class LogService {
  private apiUrl = 'http://localhost:8080/api/v1/logs';
  private http = inject(HttpClient);

  public analyzeLogText(content: string, fileName = 'pasted-log.txt'): Observable<LogAnalysisResponse> {
    return this.http.post<LogAnalysisResponse>(`${this.apiUrl}/analyze/text`, { content, fileName });
  }

  public analyzeLogFile(file: File): Observable<LogAnalysisResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<LogAnalysisResponse>(`${this.apiUrl}/analyze/upload`, formData);
  }

  public getHistory(): Observable<LogAnalysisResponse[]> {
    return this.http.get<LogAnalysisResponse[]>(`${this.apiUrl}/history`);
  }
}
