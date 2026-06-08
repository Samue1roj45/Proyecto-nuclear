import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthUser } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'psicosocial_auth';
  currentUser = signal<AuthUser | null>(this.loadUser());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(email: string, password: string) {
    return this.http.post<AuthUser>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((user) => {
        localStorage.setItem(this.storageKey, JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  register(fullName: string, email: string, password: string) {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/auth/register`, {
      fullName,
      email,
      password,
    });
  }

  forgotPassword(email: string) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/auth/forgot-password`,
      { email }
    );
  }

  oauthGoogle(code: string) {
    return this.http.post<AuthUser>(`${environment.apiUrl}/auth/oauth/google`, { code }).pipe(
      tap((user) => {
        localStorage.setItem(this.storageKey, JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  oauthFacebook(accessToken: string) {
    return this.http.post<AuthUser>(`${environment.apiUrl}/auth/oauth/facebook`, { accessToken }).pipe(
      tap((user) => {
        localStorage.setItem(this.storageKey, JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  resetPassword(email: string, resetCode: string, newPassword: string) {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/auth/reset-password`, {
      email,
      resetCode,
      newPassword,
    });
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  updateStoredUser(patch: Partial<AuthUser>): void {
    const current = this.currentUser();
    if (!current) return;
    const updated = { ...current, ...patch };
    localStorage.setItem(this.storageKey, JSON.stringify(updated));
    this.currentUser.set(updated);
  }

  get token(): string | null {
    return this.currentUser()?.token ?? null;
  }

  isAuthenticated(): boolean {
    return !!this.token;
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  homeRoute(): string {
    return this.isAdmin() ? '/admin' : '/dashboard';
  }

  private loadUser(): AuthUser | null {
    const raw = localStorage.getItem(this.storageKey);
    return raw ? JSON.parse(raw) : null;
  }
}
