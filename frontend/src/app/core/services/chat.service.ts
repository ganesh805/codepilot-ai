import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatResponse } from '../models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/v1/repos';
  private http = inject(HttpClient);

  public sendMessage(repoUuid: string, message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/${repoUuid}/chat`, { message });
  }
}
