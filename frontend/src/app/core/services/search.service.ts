import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SearchResult } from '../models/search.model';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public searchCodebase(repoUuid: string, query: string, topK = 5): Observable<SearchResult[]> {
    return this.http.post<SearchResult[]>(`${this.apiUrl}/${repoUuid}/search`, { query, topK });
  }
}
