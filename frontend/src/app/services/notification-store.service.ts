import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { AppNotification } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  unreadCount = signal(0);
  notifications = signal<AppNotification[]>([]);
  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private eventSource: EventSource | null = null;

  startPolling(): void {
    this.refresh();
    this.connectStream();
    if (!this.pollHandle) {
      this.pollHandle = setInterval(() => this.refreshCount(), 60000);
    }
  }

  stopPolling(): void {
    this.disconnectStream();
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

  private connectStream(): void {
    const token = this.auth.token;
    if (!token || this.eventSource) return;

    const url = `${environment.apiUrl}/notifications/stream?token=${encodeURIComponent(token)}`;
    const source = new EventSource(url);

    source.addEventListener('update', (event) => {
      try {
        const data = JSON.parse((event as MessageEvent).data) as { unreadCount?: number };
        if (typeof data.unreadCount === 'number') {
          this.unreadCount.set(data.unreadCount);
        }
      } catch {
        this.refreshCount();
      }
    });

    source.onerror = () => {
      this.disconnectStream();
      this.refreshCount();
      setTimeout(() => this.connectStream(), 10000);
    };

    this.eventSource = source;
  }

  private disconnectStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
