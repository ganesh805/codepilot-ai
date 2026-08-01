import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmbeddingResponse, EmbeddingStats } from '../models/embedding.model';

@Injectable({
  providedIn: 'root'
})
export class EmbeddingService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public generateEmbeddings(uuid: string): Observable<EmbeddingResponse[]> {
    return this.http.post<EmbeddingResponse[]>(`${this.apiUrl}/${uuid}/embeddings/generate`, {});
  }

  public getStats(uuid: string): Observable<EmbeddingStats> {
    return this.http.get<EmbeddingStats>(`${this.apiUrl}/${uuid}/embeddings/stats`);
  }
}
