import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AdminStats } from '../../models';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, UserAvatarComponent],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  private api = inject(ApiService);

  stats: AdminStats | null = null;
  loading = true;

  ngOnInit(): void {
    this.api.getAdminStats().subscribe({
      next: (s) => {
        this.stats = s;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  rankColor(i: number): string {
    if (i === 0) return 'text-yellow-500';
    if (i === 1) return 'text-gray-400';
    if (i === 2) return 'text-amber-700';
    return 'text-on-surface-variant';
  }
}
