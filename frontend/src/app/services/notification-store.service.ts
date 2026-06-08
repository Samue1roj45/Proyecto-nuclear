import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AppNotification } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private api = inject(ApiService);

  unreadCount = signal(0);
  notifications = signal<AppNotification[]>([]);
  private pollHandle: any = null;

  startPolling(): void {
    this.refresh();
    if (!this.pollHandle) {
      this.pollHandle = setInterval(() => this.refreshCount(), 20000);
    }
  }

  stopPolling(): void {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  refresh(): void {
    this.api.getNotifications().subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.unreadCount.set(list.filter((n) => !n.read).length);
      },
    });
  }

  refreshCount(): void {
    this.api.getUnreadCount().subscribe({
      next: (res) => this.unreadCount.set(res.count),
    });
  }

  markRead(id: number): void {
    this.api.markNotificationRead(id).subscribe({ next: () => this.refresh() });
  }

  markAllRead(): void {
    this.api.markAllNotificationsRead().subscribe({ next: () => this.refresh() });
  }

  delete(id: number): void {
    this.api.deleteNotification(id).subscribe({ next: () => this.refresh() });
  }

  clearAll(): void {
    this.api.clearNotifications().subscribe({ next: () => this.refresh() });
  }
}
