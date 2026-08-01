import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CodeRepository, GitImportRequest } from '../models/repository.model';

@Injectable({
  providedIn: 'root'
})
export class RepositoryService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public importGithub(request: GitImportRequest): Observable<CodeRepository> {
    return this.http.post<CodeRepository>(`${this.apiUrl}/import/github`, request);
  }

  public importZip(file: File): Observable<CodeRepository> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CodeRepository>(`${this.apiUrl}/import/zip`, formData);
  }

  public getRepositories(): Observable<CodeRepository[]> {
    return this.http.get<CodeRepository[]>(this.apiUrl);
  }

  public getRepository(uuid: string): Observable<CodeRepository> {
    return this.http.get<CodeRepository>(`${this.apiUrl}/${uuid}`);
  }

  public deleteRepository(uuid: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${uuid}`);
  }
}
