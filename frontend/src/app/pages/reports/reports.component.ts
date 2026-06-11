import { Component, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { AttemptDetail, AttemptSummary, ReportsSummary } from '../../models';
import { attemptStatusLabel, scoreCategoryLabel } from '../../utils/status-labels';
import { GlassSelectComponent } from '../../components/glass-select/glass-select.component';
import { ReportsPdfService } from '../../services/reports-pdf.service';

type AcademicDimensionKey = 'clinical' | 'ethical' | 'normative';

interface AcademicDimensionInsight {
  key: AcademicDimensionKey;
  label: string;
  score: number;
  tip: string;
  tone: 'cyan' | 'blue' | 'coral';
}

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
  private reportsPdf = inject(ReportsPdfService);
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
  showAcademicModal = false;

  readonly passThreshold = 60;

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

  openAcademicFocus(): void {
    this.showAcademicModal = true;
  }

  closeAcademicFocus(): void {
    this.showAcademicModal = false;
  }

  overallAverage(): number {
    if (!this.reports) return 0;
    return Math.round(
      (this.reports.avgClinical + this.reports.avgEthical + this.reports.avgNormative) / 3
    );
  }

  meetsPassThreshold(): boolean {
    return this.overallAverage() >= this.passThreshold;
  }

  pointsToPass(): number {
    return Math.max(0, this.passThreshold - this.overallAverage());
  }

  passProgress(): number {
    return Math.min(100, Math.round((this.overallAverage() / this.passThreshold) * 100));
  }

  dimensions(): AcademicDimensionInsight[] {
    if (!this.reports) return [];
    return [
      {
        key: 'clinical',
        label: 'Clínica',
        score: this.reports.avgClinical,
        tip: 'Refuerza contención emocional, evaluación del riesgo y plan de intervención inmediata.',
        tone: 'cyan',
      },
      {
        key: 'ethical',
        label: 'Ética',
        score: this.reports.avgEthical,
        tip: 'Prioriza confidencialidad, consentimiento informado y límites profesionales claros.',
        tone: 'blue',
      },
      {
        key: 'normative',
        label: 'Normativa',
        score: this.reports.avgNormative,
        tip: 'Activa protocolos legales, derivaciones institucionales y rutas de protección.',
        tone: 'coral',
      },
    ];
  }

  weakestDimension(): AcademicDimensionInsight | null {
    const dims = this.dimensions();
    if (!dims.length) return null;
    return dims.reduce((lowest, current) => (current.score < lowest.score ? current : lowest));
  }

  strongestDimension(): AcademicDimensionInsight | null {
    const dims = this.dimensions();
    if (!dims.length) return null;
    return dims.reduce((best, current) => (current.score > best.score ? current : best));
  }

  weakestAttempt(): AttemptSummary | null {
    if (!this.reports?.attempts.length) return null;
    return [...this.reports.attempts].sort((a, b) => a.totalScore - b.totalScore)[0];
  }

  academicBannerSubtitle(): string {
    const weakest = this.weakestDimension();
    if (!weakest || this.reports?.casesAttempted === 0) {
      return 'Consulta criterios y comienza tu evaluación académica.';
    }
    if (this.meetsPassThreshold()) {
      return `Vas bien. Tu fortaleza actual es ${this.strongestDimension()?.label.toLowerCase()}.`;
    }
    return `Refuerza ${weakest.label.toLowerCase()}: te faltan ${this.pointsToPass()} pts para aprobar.`;
  }

  focusWeakestAttempt(): void {
    const attempt = this.weakestAttempt();
    if (!attempt) return;
    this.selectAttempt(attempt);
    this.closeAcademicFocus();
    setTimeout(() => {
      document.getElementById('attempt-detail-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 150);
  }

  practiceWeakestCase(): void {
    const attempt = this.weakestAttempt();
    if (!attempt?.caseId) {
      this.toast.error('No hay un caso disponible para practicar todavía.');
      return;
    }
    this.closeAcademicFocus();
    this.router.navigate(['/cases', attempt.caseId]);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.showAcademicModal) this.closeAcademicFocus();
  }

  exportAll(): void {
    if (!this.reports) return;
    try {
      this.reportsPdf.exportAttemptsReport(this.reports, this.isAdmin);
      this.toast.success('Reporte exportado en PDF');
    } catch {
      this.toast.error('Error al exportar PDF');
    }
  }

  exportAcademicPdf(): void {
    if (!this.reports) return;
    const weakest = this.weakestDimension();
    try {
      this.reportsPdf.exportAcademicSummary(this.reports, {
        isAdmin: this.isAdmin,
        passThreshold: this.passThreshold,
        overallAverage: this.overallAverage(),
        passProgress: this.passProgress(),
        pointsToPass: this.pointsToPass(),
        meetsPassThreshold: this.meetsPassThreshold(),
        weakestLabel: weakest?.label,
        weakestTip: weakest?.tip,
      });
      this.toast.success('Plan académico exportado en PDF');
    } catch {
      this.toast.error('Error al exportar PDF');
    }
  }
}
