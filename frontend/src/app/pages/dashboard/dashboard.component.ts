import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { CaseCard, StudentDashboard } from '../../models';
import { studentCaseStatusLabel } from '../../utils/status-labels';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  dashboard: StudentDashboard | null = null;
  loading = true;
  message = '';

  ngOnInit(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin']);
      return;
    }
    this.route.queryParams.subscribe((params) => {
      this.load(params['search']);
    });
  }

  load(search?: string): void {
    this.loading = true;
    this.api.getDashboard(search).subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  requestReset(caseItem: CaseCard): void {
    if (caseItem.resetPending) return;
    this.api.requestReset(caseItem.id).subscribe({
      next: (res) => {
        this.message = res.message;
        caseItem.resetPending = true;
      },
      error: (err) => (this.message = err.error?.message || 'Error al solicitar reinicio'),
    });
  }

  openCase(caseItem: CaseCard): void {
    this.router.navigate(['/cases', caseItem.id]);
  }

  statusLabel(status: string): string {
    return studentCaseStatusLabel(status);
  }

  isLost(status: string): boolean {
    return status === 'FAILED' || status === 'BLOCKED';
  }

  lostLabel(status: string): string {
    return status === 'BLOCKED' ? 'Bloqueado (sin intentos)' : 'No aprobado';
  }
}
