import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { CaseAdmin, CaseFormRequest } from '../../models';
import { GlassSelectComponent } from '../../components/glass-select/glass-select.component';

@Component({
  selector: 'app-manage-cases',
  standalone: true,
  imports: [CommonModule, FormsModule, GlassSelectComponent],
  templateUrl: './manage-cases.component.html',
  styleUrl: './manage-cases.component.scss',
})
export class ManageCasesComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  cases: CaseAdmin[] = [];
  loading = true;
  search = '';

  showModal = false;
  editingId: number | null = null;
  form: CaseFormRequest = this.emptyForm();
  competencyInput = '';

  readonly levelOptions = [
    { value: 'Nivel Básico', label: 'Nivel Básico' },
    { value: 'Nivel Intermedio', label: 'Nivel Intermedio' },
    { value: 'Nivel Avanzado', label: 'Nivel Avanzado' },
  ];

  readonly defaultImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCjkwslr8U4Zg1XklPZ8d_U9BW9rWhEnZYMBjtS5_-jA2teROU9uoahJgmOj-HYyXQZFZbsuCesZtkUV00y_E6HolZnla_fESU_TPVQZO0j9bZm9LLVOMrdoqPHcS9dM1_ikMbSBPqOjGGsP_Ug6XWafrw7ii308Fbu8evZE9XELTW_7zfrr0beKA5FYo-iqVibHGFO0xYNrjF0GxgZnqeYsj7gffBjt8WKnAXSeMKYSc_1dsXGj9LF9qepZSdq-FSie5gDZ_Hxqe8';

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.search = params['search'] || '';
      this.load();
    });
  }

  emptyForm(): CaseFormRequest {
    return {
      title: '',
      description: '',
      category: '',
      level: 'Nivel Intermedio',
      imageUrl: '',
      contextQuote: '',
      estimatedMinutes: 45,
      complexityStars: 3,
      competencies: [],
    };
  }

  load(): void {
    this.loading = true;
    this.api.getAdminCases(this.search).subscribe({
      next: (list) => {
        this.cases = list;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.competencyInput = '';
    this.showModal = true;
  }

  openEdit(c: CaseAdmin): void {
    this.editingId = c.id;
    this.form = {
      title: c.title,
      description: c.description,
      category: c.category,
      level: c.level,
      imageUrl: c.imageUrl,
      contextQuote: c.contextQuote,
      estimatedMinutes: c.estimatedMinutes,
      complexityStars: c.complexityStars,
      competencies: [...(c.competencies || [])],
    };
    this.competencyInput = '';
    this.showModal = true;
  }

  addCompetency(): void {
    const v = this.competencyInput.trim();
    if (v && !this.form.competencies.includes(v)) {
      this.form.competencies.push(v);
    }
    this.competencyInput = '';
  }

  removeCompetency(c: string): void {
    this.form.competencies = this.form.competencies.filter((x) => x !== c);
  }

  save(): void {
    if (!this.form.title.trim()) {
      this.toast.error('El título es obligatorio');
      return;
    }
    if (!this.form.imageUrl.trim()) {
      this.form.imageUrl = this.defaultImage;
    }
    const req$ = this.editingId
      ? this.api.updateCase(this.editingId, this.form)
      : this.api.createCase(this.form);

    req$.subscribe({
      next: () => {
        this.toast.success(this.editingId ? 'Caso actualizado' : 'Caso creado');
        this.showModal = false;
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al guardar'),
    });
  }

  remove(c: CaseAdmin): void {
    if (!confirm(`¿Eliminar el caso "${c.title}"? Se borrarán sus intentos asociados.`)) return;
    this.api.deleteCase(c.id).subscribe({
      next: () => {
        this.toast.success('Caso eliminado');
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al eliminar'),
    });
  }

  openSimulator(c: CaseAdmin): void {
    this.router.navigate(['/cases', c.id]);
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).src = this.defaultImage;
  }
}
