import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyticsMetrics } from '../models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = 'http://localhost:8080/api/v1/analytics';
  private http = inject(HttpClient);

  public getDashboardMetrics(): Observable<AnalyticsMetrics> {
    return this.http.get<AnalyticsMetrics>(`${this.apiUrl}/dashboard`);
  }
}
