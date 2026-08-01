import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CodeChunk, ScanStats } from '../models/chunk.model';

@Injectable({
  providedIn: 'root'
})
export class ScannerService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public scanRepository(uuid: string): Observable<CodeChunk[]> {
    return this.http.post<CodeChunk[]>(`${this.apiUrl}/${uuid}/scan`, {});
  }

  public getChunks(uuid: string): Observable<CodeChunk[]> {
    return this.http.get<CodeChunk[]>(`${this.apiUrl}/${uuid}/chunks`);
  }

  public getStats(uuid: string): Observable<ScanStats> {
    return this.http.get<ScanStats>(`${this.apiUrl}/${uuid}/chunks/stats`);
  }
}
