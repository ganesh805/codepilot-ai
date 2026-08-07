import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CodeOptimizerRequest, CodeOptimizerResponse } from '../models/optimizer.model';

@Injectable({
  providedIn: 'root'
})
export class OptimizerService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/optimizer';

  optimizeCode(request: CodeOptimizerRequest): Observable<CodeOptimizerResponse> {
    return this.http.post<CodeOptimizerResponse>(`${this.apiUrl}/analyze`, request);
  }

  getHistory(): Observable<CodeOptimizerResponse[]> {
    return this.http.get<CodeOptimizerResponse[]>(`${this.apiUrl}/history`);
  }
}
