import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { UserDetail } from '../../models';
import { attemptStatusLabel } from '../../utils/status-labels';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, UserAvatarComponent],
  templateUrl: './user-detail.component.html',
})
export class UserDetailComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);

  detail: UserDetail | null = null;
  loading = true;
  readonly attemptStatusLabel = attemptStatusLabel;

  ngOnInit(): void {
    const id = +this.route.snapshot.params['id'];
    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.api.getUserDetail(id).subscribe({
      next: (d) => {
        this.detail = d;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  resetAttempts(): void {
    if (!this.detail) return;
    const name = this.detail.user.fullName;
    const msg =
      `¿Reiniciar TODOS los intentos de ${name} en todos los casos?\n\n` +
      'También se aprobarán las solicitudes de reinicio pendientes.';
    if (!confirm(msg)) return;
    this.api.resetUserAttempts(this.detail.user.id).subscribe({
      next: () => {
        this.toast.success('Todos los intentos reiniciados');
        this.load(this.detail!.user.id);
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al reiniciar intentos'),
    });
  }

  toggleEnabled(): void {
    if (!this.detail) return;
    this.api.setUserEnabled(this.detail.user.id, !this.detail.user.enabled).subscribe({
      next: (u) => {
        this.detail!.user.enabled = u.enabled;
        this.toast.success(u.enabled ? 'Usuario habilitado' : 'Usuario deshabilitado');
      },
    });
  }
}
