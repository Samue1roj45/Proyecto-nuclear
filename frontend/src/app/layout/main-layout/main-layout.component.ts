import { Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AuthTransitionService } from '../../services/auth-transition.service';
import { NotificationStore } from '../../services/notification-store.service';
import { ToastComponent } from '../../components/toast/toast.component';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';
import { AppNotification } from '../../models';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, FormsModule, ToastComponent, UserAvatarComponent],
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  authTransition = inject(AuthTransitionService);
  router = inject(Router);
  notifications = inject(NotificationStore);

  searchQuery = '';
  showNotifications = false;
  showProfileMenu = false;

  ngOnInit(): void {
    this.notifications.startPolling();
  }

  ngOnDestroy(): void {
    this.notifications.stopPolling();
  }

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  get searchPlaceholder(): string {
    const url = this.router.url;
    if (url.startsWith('/reports')) return 'Buscar reportes...';
    if (url.startsWith('/users')) return 'Buscar usuarios...';
    if (url.startsWith('/groups')) return 'Buscar grupos...';
    if (url.startsWith('/manage-cases')) return 'Buscar casos...';
    if (url.startsWith('/reset-requests')) return 'Buscar solicitudes...';
    if (url.startsWith('/admin')) return 'Ir a usuarios para buscar...';
    return 'Buscar casos...';
  }

  get searchEnabled(): boolean {
    const url = this.router.url;
    return (
      url.startsWith('/reports') ||
      url.startsWith('/users') ||
      url.startsWith('/dashboard') ||
      url.startsWith('/groups') ||
      url.startsWith('/manage-cases')
    );
  }

  onSearch(): void {
    const q = this.searchQuery.trim();
    const url = this.router.url;
    if (url.startsWith('/reports')) {
      this.router.navigate(['/reports'], { queryParams: { search: q || null } });
    } else if (url.startsWith('/users')) {
      this.router.navigate(['/users'], { queryParams: { search: q || null } });
    } else if (url.startsWith('/groups')) {
      this.router.navigate(['/groups'], { queryParams: { search: q || null } });
    } else if (url.startsWith('/manage-cases')) {
      this.router.navigate(['/manage-cases'], { queryParams: { search: q || null } });
    } else {
      this.router.navigate(['/dashboard'], { queryParams: { search: q || null } });
    }
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.showNotifications = !this.showNotifications;
    this.showProfileMenu = false;
    if (this.showNotifications) this.notifications.refresh();
  }

  toggleProfileMenu(event: Event): void {
    event.stopPropagation();
    this.showProfileMenu = !this.showProfileMenu;
    this.showNotifications = false;
  }

  @HostListener('document:click')
  closeMenus(): void {
    this.showNotifications = false;
    this.showProfileMenu = false;
  }

  openNotification(n: AppNotification): void {
    if (!n.read) this.notifications.markRead(n.id);
    this.showNotifications = false;
    if (n.link) this.router.navigateByUrl(n.link);
  }

  notificationIcon(type: string): string {
    const map: Record<string, string> = {
      INFO: 'info',
      SUCCESS: 'check_circle',
      WARNING: 'warning',
      ERROR: 'error',
      RESET_REQUEST: 'restart_alt',
      RESET_APPROVED: 'task_alt',
      CASE_COMPLETED: 'emoji_events',
      ACCOUNT: 'person',
    };
    return map[type] || 'notifications';
  }

  logout(): void {
    this.notifications.stopPolling();
    this.auth.logout();
  }
}
