import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/v1/auth';
  private http = inject(HttpClient);

  public currentUser = signal<User | null>(null);
  public isAuthenticated = signal<boolean>(false);

  constructor() {
    this.checkInitialAuthState();
  }

  private checkInitialAuthState(): void {
    const token = this.getToken();
    if (token) {
      this.fetchCurrentUser().subscribe({
        next: (user) => {
          this.currentUser.set(user);
          this.isAuthenticated.set(true);
        },
        error: () => {
          this.logout();
        }
      });
    }
  }

  public register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap((res) => this.handleAuthSuccess(res))
    );
  }

  public login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((res) => this.handleAuthSuccess(res))
    );
  }

  public fetchCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  public hasRole(roleName: string): boolean {
    const user = this.currentUser();
    return user ? user.roles?.includes(roleName) || false : false;
  }

  public isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  public isDeveloper(): boolean {
    return this.hasRole('ROLE_DEVELOPER');
  }

  public logout(): void {
    localStorage.removeItem('codepilot_token');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  public getToken(): string | null {
    return localStorage.getItem('codepilot_token');
  }

  private handleAuthSuccess(res: AuthResponse): void {
    localStorage.setItem('codepilot_token', res.accessToken);
    this.currentUser.set(res.user);
    this.isAuthenticated.set(true);
  }
}
