import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { NotificationStore } from '../../services/notification-store.service';
import { ResetRequestSummary } from '../../models';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-reset-requests',
  standalone: true,
  imports: [CommonModule, UserAvatarComponent],
  templateUrl: './reset-requests.component.html',
})
export class ResetRequestsComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private store = inject(NotificationStore);

  requests: ResetRequestSummary[] = [];
  loading = true;
  pendingOnly = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.getResetRequests(this.pendingOnly).subscribe({
      next: (list) => {
        this.requests = list;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  togglePending(): void {
    this.pendingOnly = !this.pendingOnly;
    this.load();
  }

  approve(r: ResetRequestSummary): void {
    this.api.approveReset(r.id).subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.load();
        this.store.refreshCount();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error'),
    });
  }

  reject(r: ResetRequestSummary): void {
    this.api.rejectReset(r.id).subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error'),
    });
  }

  initials(name: string): string {
    return name.split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase();
  }
}
