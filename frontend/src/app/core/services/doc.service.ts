import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiDocResponse } from '../models/doc.model';

@Injectable({
  providedIn: 'root'
})
export class DocService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public generateDocs(repoUuid: string): Observable<ApiDocResponse> {
    return this.http.post<ApiDocResponse>(`${this.apiUrl}/${repoUuid}/docs/generate`, {});
  }

  public getDocs(repoUuid: string): Observable<ApiDocResponse> {
    return this.http.get<ApiDocResponse>(`${this.apiUrl}/${repoUuid}/docs`);
  }
}
