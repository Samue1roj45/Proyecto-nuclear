import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { NotificationStore } from '../../services/notification-store.service';
import { ToastService } from '../../services/toast.service';
import { AppNotification } from '../../models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
})
export class NotificationsComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private toast = inject(ToastService);
  store = inject(NotificationStore);

  notifications: AppNotification[] = [];
  filter: 'all' | 'unread' = 'all';
  loading = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.getNotifications(this.filter === 'unread').subscribe({
      next: (list) => {
        this.notifications = list;
        this.loading = false;
        this.store.refreshCount();
      },
      error: () => (this.loading = false),
    });
  }

  setFilter(f: 'all' | 'unread'): void {
    this.filter = f;
    this.load();
  }

  open(n: AppNotification): void {
    if (!n.read) {
      this.api.markNotificationRead(n.id).subscribe({ next: () => this.store.refreshCount() });
      n.read = true;
    }
    if (n.link) this.router.navigateByUrl(n.link);
  }

  markAll(): void {
    this.api.markAllNotificationsRead().subscribe({
      next: () => {
        this.toast.success('Todas marcadas como leídas');
        this.load();
        this.store.refreshCount();
      },
    });
  }

  remove(n: AppNotification, event: Event): void {
    event.stopPropagation();
    this.api.deleteNotification(n.id).subscribe({
      next: () => {
        this.toast.success('Notificación eliminada');
        this.load();
      },
    });
  }

  clearAll(): void {
    this.api.clearNotifications().subscribe({
      next: () => {
        this.toast.success('Notificaciones eliminadas');
        this.load();
        this.store.refreshCount();
      },
    });
  }

  icon(type: string): string {
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
}
