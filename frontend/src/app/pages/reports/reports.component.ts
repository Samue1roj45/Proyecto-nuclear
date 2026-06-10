import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { AttemptDetail, AttemptSummary, ReportsSummary } from '../../models';
import { attemptStatusLabel, scoreCategoryLabel } from '../../utils/status-labels';
import { GlassSelectComponent } from '../../components/glass-select/glass-select.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, GlassSelectComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);
  private auth = inject(AuthService);
  router = inject(Router);

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  reports: ReportsSummary | null = null;
  selectedAttempt: AttemptSummary | null = null;
  detail: AttemptDetail | null = null;
  loading = true;

  search = '';
  statusFilter = 'ALL';
  sortBy = 'date';
  page = 0;
  pageSize = 20;

  readonly statusFilterOptions = [
    { value: 'ALL', label: 'Todos los estados' },
    { value: 'PASSED', label: 'Aprobados' },
    { value: 'FAILED', label: 'No aprobados (con intentos)' },
    { value: 'BLOCKED', label: 'Bloqueados (sin intentos)' },
  ];

  readonly sortOptions = [
    { value: 'date', label: 'Más recientes' },
    { value: 'score', label: 'Mayor puntaje' },
    { value: 'scoreAsc', label: 'Menor puntaje' },
    { value: 'name', label: 'Nombre (A-Z)' },
  ];

  statusLabel = attemptStatusLabel;
  categoryLabel = scoreCategoryLabel;

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.search = params['search'] || '';
      this.page = 0;
      this.load();
    });
  }

  get totalPages(): number {
    const total = this.reports?.totalAttempts ?? 0;
    return Math.max(1, Math.ceil(total / this.pageSize));
  }

  load(): void {
    this.loading = true;
    this.api.getReports(this.search, this.statusFilter, this.sortBy, this.page, this.pageSize).subscribe({
      next: (data) => {
        this.reports = data;
        if (data.attempts.length > 0) {
          this.selectAttempt(data.attempts[0]);
        } else {
          this.selectedAttempt = null;
          this.detail = null;
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err.error?.message || 'No se pudieron cargar los reportes');
      },
    });
  }

  changePage(delta: number): void {
    const next = this.page + delta;
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.load();
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  selectAttempt(attempt: AttemptSummary): void {
    this.selectedAttempt = attempt;
    this.api.getAttemptFullDetail(attempt.id).subscribe({
      next: (d) => (this.detail = d),
    });
  }

  ringOffset(score: number): number {
    const circumference = 251.2;
    return circumference - (score / 100) * circumference;
  }

  exportAll(): void {
    this.api.exportReportsCsv(this.search, this.statusFilter).subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'reportes.csv';
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('Reporte exportado a CSV');
      },
      error: () => this.toast.error('Error al exportar'),
    });
  }
}
