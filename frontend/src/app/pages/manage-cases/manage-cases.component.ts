import { Component, OnInit, inject } from '@angular/core';

import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';

import { ActivatedRoute, Router } from '@angular/router';

import { ApiService } from '../../services/api.service';

import { ConfirmDialogService } from '../../services/confirm-dialog.service';

import { ToastService } from '../../services/toast.service';

import { CaseAdmin, CaseFormRequest, QuestionAdmin, QuestionFormRequest } from '../../models';

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

  private confirmDialog = inject(ConfirmDialogService);

  private router = inject(Router);

  private route = inject(ActivatedRoute);



  cases: CaseAdmin[] = [];

  loading = true;

  search = '';



  showModal = false;

  showQuestionsModal = false;

  editingId: number | null = null;

  questionsCaseId: number | null = null;

  questionsCaseTitle = '';

  questions: QuestionAdmin[] = [];

  questionsLoading = false;

  editingQuestionId: number | null = null;



  form: CaseFormRequest = this.emptyForm();

  questionForm: QuestionFormRequest = this.emptyQuestionForm();

  competencyInput = '';



  readonly levelOptions = [

    { value: 'Nivel Básico', label: 'Nivel Básico' },

    { value: 'Nivel Intermedio', label: 'Nivel Intermedio' },

    { value: 'Nivel Avanzado', label: 'Nivel Avanzado' },

  ];



  readonly categoryOptions = [

    { value: 'CLINICAL', label: 'Clínica' },

    { value: 'ETHICAL', label: 'Ética' },

    { value: 'NORMATIVE', label: 'Normativa' },

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

      title: '', description: '', category: '', level: 'Nivel Intermedio',

      imageUrl: '', contextQuote: '', estimatedMinutes: 45, complexityStars: 3,

      competencies: [], timerEnabled: false,

    };

  }



  emptyQuestionForm(): QuestionFormRequest {

    return {

      text: '', sceneImageUrl: '', sceneTitle: '', sceneSubtitle: '', sceneHint: '', npcLabel: '',

      options: [

        { text: '', correct: true, category: 'CLINICAL', feedback: '' },

        { text: '', correct: false, category: 'CLINICAL', feedback: '' },

        { text: '', correct: false, category: 'CLINICAL', feedback: '' },

      ],

    };

  }



  load(): void {

    this.loading = true;

    this.api.getAdminCases(this.search).subscribe({

      next: (list) => { this.cases = list; this.loading = false; },

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

      title: c.title, description: c.description, category: c.category, level: c.level,

      imageUrl: c.imageUrl, contextQuote: c.contextQuote, estimatedMinutes: c.estimatedMinutes,

      complexityStars: c.complexityStars, competencies: [...(c.competencies || [])],

      timerEnabled: c.timerEnabled ?? false,

    };

    this.competencyInput = '';

    this.showModal = true;

  }



  openQuestions(c: CaseAdmin): void {

    this.questionsCaseId = c.id;

    this.questionsCaseTitle = c.title;

    this.questionForm = this.emptyQuestionForm();

    this.editingQuestionId = null;

    this.showQuestionsModal = true;

    this.loadQuestions();

  }



  loadQuestions(): void {

    if (!this.questionsCaseId) return;

    this.questionsLoading = true;

    this.api.getCaseQuestions(this.questionsCaseId).subscribe({

      next: (list) => { this.questions = list; this.questionsLoading = false; },

      error: () => (this.questionsLoading = false),

    });

  }



  addCompetency(): void {

    const v = this.competencyInput.trim();

    if (v && !this.form.competencies.includes(v)) this.form.competencies.push(v);

    this.competencyInput = '';

  }



  removeCompetency(c: string): void {

    this.form.competencies = this.form.competencies.filter((x) => x !== c);

  }



  save(): void {

    if (!this.form.title.trim()) { this.toast.error('El título es obligatorio'); return; }

    if (!this.form.imageUrl.trim()) this.form.imageUrl = this.defaultImage;

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



  saveQuestion(): void {

    if (!this.questionsCaseId) return;

    if (!this.questionForm.text.trim()) { this.toast.error('El texto de la pregunta es obligatorio'); return; }

    const validOpts = this.questionForm.options.filter((o) => o.text.trim());

    if (validOpts.length < 2) { this.toast.error('Agrega al menos 2 opciones'); return; }

    if (!validOpts.some((o) => o.correct)) { this.toast.error('Marca una opción como correcta'); return; }



    const payload = { ...this.questionForm, options: validOpts };

    if (this.editingQuestionId) {
      this.api.updateQuestion(this.editingQuestionId, payload).subscribe({
        next: () => this.onQuestionSaved('Pregunta actualizada'),
        error: (err: { error?: { message?: string } }) =>
          this.toast.error(err.error?.message || 'Error al guardar pregunta'),
      });
      return;
    }

    this.api.addQuestion(this.questionsCaseId, payload).subscribe({
      next: () => this.onQuestionSaved('Pregunta agregada'),
      error: (err: { error?: { message?: string } }) =>
        this.toast.error(err.error?.message || 'Error al guardar pregunta'),
    });
  }

  private onQuestionSaved(message: string): void {
    this.toast.success(message);
    this.questionForm = this.emptyQuestionForm();
    this.editingQuestionId = null;
    this.loadQuestions();
    this.load();
  }

  editQuestion(q: QuestionAdmin): void {

    this.editingQuestionId = q.id;

    this.questionForm = {

      text: q.text,

      sceneImageUrl: q.sceneImageUrl || '',

      sceneTitle: q.sceneTitle || '',

      sceneSubtitle: q.sceneSubtitle || '',

      sceneHint: q.sceneHint || '',

      npcLabel: q.npcLabel || '',

      options: (q.options || []).map((o) => ({

        text: o.text,

        correct: o.correct,

        category: o.category || 'CLINICAL',

        feedback: o.feedback || '',

      })),

    };

    if (this.questionForm.options.length < 2) {

      while (this.questionForm.options.length < 2) {

        this.questionForm.options.push({ text: '', correct: false, category: 'CLINICAL', feedback: '' });

      }

    }

  }

  cancelEditQuestion(): void {

    this.editingQuestionId = null;

    this.questionForm = this.emptyQuestionForm();

  }

  moveQuestionUp(index: number): void {

    if (index <= 0 || !this.questionsCaseId) return;

    const ids = this.questions.map((q) => q.id);

    [ids[index - 1], ids[index]] = [ids[index], ids[index - 1]];

    this.applyReorder(ids);

  }

  moveQuestionDown(index: number): void {

    if (index >= this.questions.length - 1 || !this.questionsCaseId) return;

    const ids = this.questions.map((q) => q.id);

    [ids[index], ids[index + 1]] = [ids[index + 1], ids[index]];

    this.applyReorder(ids);

  }

  private applyReorder(questionIds: number[]): void {

    if (!this.questionsCaseId) return;

    this.api.reorderQuestions(this.questionsCaseId, questionIds).subscribe({

      next: () => this.loadQuestions(),

      error: (err) => this.toast.error(err.error?.message || 'Error al reordenar'),

    });

  }



  async deleteQuestion(q: QuestionAdmin): Promise<void> {

    const confirmed = await this.confirmDialog.confirm({

      title: 'Eliminar pregunta',

      message: `¿Eliminar la pregunta "${q.text.substring(0, 50)}..."?`,

      confirmLabel: 'Eliminar', variant: 'danger',

    });

    if (!confirmed) return;

    this.api.deleteQuestion(q.id).subscribe({

      next: () => { this.toast.success('Pregunta eliminada'); this.loadQuestions(); this.load(); },

      error: (err) => this.toast.error(err.error?.message || 'Error al eliminar'),

    });

  }



  async remove(c: CaseAdmin): Promise<void> {

    const confirmed = await this.confirmDialog.confirm({

      title: 'Eliminar caso',

      message: `¿Eliminar el caso "${c.title}"? Se borrarán sus intentos asociados.`,

      confirmLabel: 'Eliminar', variant: 'danger',

    });

    if (!confirmed) return;

    this.api.deleteCase(c.id).subscribe({

      next: () => { this.toast.success('Caso eliminado'); this.load(); },

      error: (err) => this.toast.error(err.error?.message || 'Error al eliminar'),

    });

  }



  openSimulator(c: CaseAdmin): void {

    this.router.navigate(['/cases', c.id], { queryParams: { preview: 'true' } });

  }



  setCorrectOption(index: number): void {
    this.questionForm.options.forEach((o, i) => (o.correct = i === index));
  }

  onImgError(event: Event): void {

    (event.target as HTMLImageElement).src = this.defaultImage;

  }

}

