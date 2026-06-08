import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { CaseDetail } from '../../models';
import { studentCaseStatusLabel } from '../../utils/status-labels';
import { CaseGameComponent } from '../../components/case-game/case-game.component';

@Component({
  selector: 'app-case-simulator',
  standalone: true,
  imports: [CommonModule, RouterLink, CaseGameComponent],
  templateUrl: './case-simulator.component.html',
})
export class CaseSimulatorComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  caseDetail: CaseDetail | null = null;
  loading = true;
  message = '';
  launched = false;

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.launched = false;
      this.loadCase(+params['id']);
    });
  }

  loadCase(id: number): void {
    this.loading = true;
    this.api.getCase(id).subscribe({
      next: (data) => {
        this.caseDetail = data;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  launchSimulation(): void {
    if (!this.caseDetail) return;
    if (this.caseDetail.blocked && !this.isAdmin) {
      this.toast.error('Caso bloqueado. Solicita reinicio al profesor.');
      return;
    }
    if (this.caseDetail.activeAttemptId) {
      this.launched = true;
      return;
    }
    this.api.startCase(this.caseDetail.id).subscribe({
      next: (data) => {
        this.caseDetail = data;
        this.launched = true;
      },
      error: (err) => this.toast.error(err.error?.message || 'No se pudo iniciar la simulación'),
    });
  }

  closeSimulation(): void {
    this.launched = false;
    if (this.caseDetail) this.loadCase(this.caseDetail.id);
  }

  onCaseUpdated(data: CaseDetail): void {
    this.caseDetail = data;
  }

  requestReset(): void {
    if (!this.caseDetail || this.caseDetail.resetPending) return;
    this.api.requestReset(this.caseDetail.id).subscribe({
      next: (res) => {
        this.message = res.message;
        this.caseDetail!.resetPending = true;
        this.toast.success(res.message);
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al solicitar reinicio'),
    });
  }

  starsArray(): number[] {
    return Array.from({ length: 5 }, (_, i) => i);
  }

  isFilledStar(i: number): boolean {
    const stars = this.caseDetail?.complexityStars || 0;
    return i < Math.floor(stars);
  }

  isHalfStar(i: number): boolean {
    const stars = this.caseDetail?.complexityStars || 0;
    return i === Math.floor(stars) && stars % 1 >= 0.5;
  }

  statusLabel(): string {
    return studentCaseStatusLabel(this.caseDetail?.studentStatus || 'AVAILABLE');
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).src =
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCjkwslr8U4Zg1XklPZ8d_U9BW9rWhEnZYMBjtS5_-jA2teROU9uoahJgmOj-HYyXQZFZbsuCesZtkUV00y_E6HolZnla_fESU_TPVQZO0j9bZm9LLVOMrdoqPHcS9dM1_ikMbSBPqOjGGsP_Ug6XWafrw7ii308Fbu8evZE9XELTW_7zfrr0beKA5FYo-iqVibHGFO0xYNrjF0GxgZnqeYsj7gffBjt8WKnAXSeMKYSc_1dsXGj9LF9qepZSdq-FSie5gDZ_Hxqe8';
  }
}
