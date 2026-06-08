import { Routes } from '@angular/router';
import { adminGuard, authGuard, loginGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [loginGuard],
  },
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'cases/:id',
        loadComponent: () => import('./pages/case-simulator/case-simulator.component').then((m) => m.CaseSimulatorComponent),
      },
      {
        path: 'reports',
        loadComponent: () => import('./pages/reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'notifications',
        loadComponent: () => import('./pages/notifications/notifications.component').then((m) => m.NotificationsComponent),
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/users.component').then((m) => m.UsersComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'groups',
        loadComponent: () => import('./pages/groups/groups.component').then((m) => m.GroupsComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'users/:id',
        loadComponent: () => import('./pages/user-detail/user-detail.component').then((m) => m.UserDetailComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'reset-requests',
        loadComponent: () => import('./pages/reset-requests/reset-requests.component').then((m) => m.ResetRequestsComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'manage-cases',
        loadComponent: () => import('./pages/manage-cases/manage-cases.component').then((m) => m.ManageCasesComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'admin',
        loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
        canActivate: [adminGuard],
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
