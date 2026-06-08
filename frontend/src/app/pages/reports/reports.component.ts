import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { AttemptDetail, AttemptSummary, ReportsSummary } from '../../models';
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

  readonly statusFilterOptions = [
    { value: 'ALL', label: 'Todos los estados' },
    { value: 'PASSED', label: 'Aprobados' },
    { value: 'FAILED', label: 'No aprobados' },
    { value: 'BLOCKED', label: 'Bloqueados' },
  ];

  readonly sortOptions = [
    { value: 'date', label: 'Más recientes' },
    { value: 'score', label: 'Mayor puntaje' },
    { value: 'scoreAsc', label: 'Menor puntaje' },
    { value: 'name', label: 'Nombre (A-Z)' },
  ];

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.search = params['search'] || '';
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.api.getReports(this.search, this.statusFilter, this.sortBy).subscribe({
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
      error: () => (this.loading = false),
    });
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
