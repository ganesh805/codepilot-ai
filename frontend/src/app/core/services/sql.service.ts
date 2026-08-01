import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SqlQueryResponse } from '../models/sql.model';

@Injectable({
  providedIn: 'root'
})
export class SqlService {
  private apiUrl = 'http://localhost:8080/api/v1/sql';
  private http = inject(HttpClient);

  public optimizeQuery(rawSql: string, repositoryUuid?: string): Observable<SqlQueryResponse> {
    return this.http.post<SqlQueryResponse>(`${this.apiUrl}/optimize`, { rawSql, repositoryUuid });
  }

  public getHistory(): Observable<SqlQueryResponse[]> {
    return this.http.get<SqlQueryResponse[]>(`${this.apiUrl}/history`);
  }
}
