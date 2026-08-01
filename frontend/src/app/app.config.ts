import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, Routes } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { AdminPanelComponent } from './features/admin/admin-panel/admin-panel.component';
import { RepoListComponent } from './features/repository/repo-list/repo-list.component';
import { RepoImportComponent } from './features/repository/repo-import/repo-import.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin', component: AdminPanelComponent, canActivate: [roleGuard(['ROLE_ADMIN'])] },
  { path: 'repositories', component: RepoListComponent, canActivate: [authGuard] },
  { path: 'repositories/import', component: RepoImportComponent, canActivate: [authGuard] },
  { path: '', redirectTo: 'repositories', pathMatch: 'full' }
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync()
  ]
};
