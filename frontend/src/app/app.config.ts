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
import { RepoScannerComponent } from './features/repository/repo-scanner/repo-scanner.component';
import { RepoEmbeddingComponent } from './features/repository/repo-embedding/repo-embedding.component';
import { RepoSearchComponent } from './features/repository/repo-search/repo-search.component';
import { RepoChatComponent } from './features/repository/repo-chat/repo-chat.component';
import { ExceptionAnalyzerComponent } from './features/debugger/exception-analyzer/exception-analyzer.component';
import { LogAnalyzerComponent } from './features/logs/log-analyzer/log-analyzer.component';
import { CodeReviewerComponent } from './features/reviewer/code-reviewer/code-reviewer.component';
import { ApiDocGeneratorComponent } from './features/docs/api-doc-generator/api-doc-generator.component';
import { SqlOptimizerComponent } from './features/sql/sql-optimizer/sql-optimizer.component';
import { AnalyticsDashboardComponent } from './features/analytics/analytics-dashboard/analytics-dashboard.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin', component: AdminPanelComponent, canActivate: [roleGuard(['ROLE_ADMIN'])] },
  { path: 'repositories', component: RepoListComponent, canActivate: [authGuard] },
  { path: 'repositories/import', component: RepoImportComponent, canActivate: [authGuard] },
  { path: 'repositories/:uuid/scan', component: RepoScannerComponent, canActivate: [authGuard] },
  { path: 'repositories/:uuid/embedding', component: RepoEmbeddingComponent, canActivate: [authGuard] },
  { path: 'repositories/:uuid/search', component: RepoSearchComponent, canActivate: [authGuard] },
  { path: 'repositories/:uuid/chat', component: RepoChatComponent, canActivate: [authGuard] },
  { path: 'repositories/:uuid/docs', component: ApiDocGeneratorComponent, canActivate: [authGuard] },
  { path: 'debugger', component: ExceptionAnalyzerComponent, canActivate: [authGuard] },
  { path: 'logs', component: LogAnalyzerComponent, canActivate: [authGuard] },
  { path: 'reviews', component: CodeReviewerComponent, canActivate: [authGuard] },
  { path: 'sql-optimizer', component: SqlOptimizerComponent, canActivate: [authGuard] },
  { path: 'analytics', component: AnalyticsDashboardComponent, canActivate: [authGuard] },
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
