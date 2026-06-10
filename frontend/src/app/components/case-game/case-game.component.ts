import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { AnswerFeedback, AttemptResult, CaseDetail, Question } from '../../models';
import { scoreCategoryLabel } from '../../utils/status-labels';

interface Vec2 { x: number; y: number; }
interface Wall { x: number; y: number; w: number; h: number; }
interface Npc { id: string; x: number; y: number; color: string; label: string; pulse?: boolean; }
interface SceneConfig {
  title: string;
  subtitle: string;
  hint: string;
  floorColor: string;
  accentColor: string;
  playerStart: Vec2;
  target: Vec2 & { radius: number; label: string };
  walls: Wall[];
  npcs: Npc[];
  props: { x: number; y: number; w: number; h: number; color: string; label?: string }[];
}

const SCENE_PALETTES = [
  { floor: '#0f2d35', accent: '#00d4f0' },
  { floor: '#122830', accent: '#5eeaff' },
  { floor: '#0d2630', accent: '#ffd166' },
  { floor: '#1a2f28', accent: '#7ee787' },
  { floor: '#2a1f35', accent: '#d4a5ff' },
];

const DEFAULT_WALLS: Wall[] = [
  { x: 0, y: 0, w: 960, h: 40 },
  { x: 0, y: 500, w: 960, h: 40 },
  { x: 0, y: 0, w: 40, h: 540 },
  { x: 920, y: 0, w: 40, h: 540 },
];

@Component({
  selector: 'app-case-game',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './case-game.component.html',
  styleUrl: './case-game.component.scss',
})
export class CaseGameComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) caseDetail!: CaseDetail;
  @Input() previewMode = false;
  @Input() previewQuestions: Question[] = [];
  @Output() caseUpdated = new EventEmitter<CaseDetail>();
  @Output() closed = new EventEmitter<void>();

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('gameRoot') gameRootRef!: ElementRef<HTMLDivElement>;

  private api = inject(ApiService);
  private toast = inject(ToastService);

  private ctx!: CanvasRenderingContext2D;
  private animId = 0;
  private keys = new Set<string>();
  private player: Vec2 = { x: 0, y: 0 };
  private playerRadius = 18;
  private phase = 0;
  private pulse = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;
  private sceneImage: HTMLImageElement | null = null;
  private sceneImageUrl = '';
  submitting = false;

  gamePhase: 'intro' | 'playing' | 'decision' | 'feedback' | 'transition' | 'finished' = 'intro';
  isFullscreen = false;
  showHud = true;
  moveHint = true;
  resultMessage = '';
  resultPassed = false;
  lastFeedback: AnswerFeedback | null = null;
  attemptResult: AttemptResult | null = null;
  elapsedSeconds = 0;

  readonly width = 960;
  readonly height = 540;
  get passThreshold(): number {
    return this.caseDetail.passThreshold ?? 60;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseDetail']?.currentValue) {
      const detail = changes['caseDetail'].currentValue as CaseDetail;
      this.phase = detail.currentQuestionIndex ?? this.phase;
      this.elapsedSeconds = detail.elapsedSeconds ?? this.elapsedSeconds;
    }
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    this.phase = this.caseDetail.currentQuestionIndex ?? 0;
    this.elapsedSeconds = this.caseDetail.elapsedSeconds ?? 0;

    if (this.caseDetail.studentStatus === 'PASSED' || this.caseDetail.studentStatus === 'FAILED' || this.caseDetail.studentStatus === 'BLOCKED') {
      if (!this.caseDetail.currentQuestion) {
        this.gamePhase = 'finished';
        this.resultPassed = this.caseDetail.studentStatus === 'PASSED';
        this.resultMessage = this.resultPassed
          ? 'Completaste el caso exitosamente.'
          : 'Intento finalizado.';
      }
    } else if (this.caseDetail.activeAttemptId && this.caseDetail.currentQuestion) {
      this.gamePhase = 'playing';
      this.moveHint = false;
      this.startTimer();
    }
    this.loadScene();
    this.loop();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animId);
    this.stopTimer();
    if (document.fullscreenElement) document.exitFullscreen().catch(() => undefined);
  }

  get currentScene(): SceneConfig {
    return this.buildScene(this.phase);
  }

  get currentQuestion(): Question | null {
    if (this.previewMode && this.previewQuestions.length) {
      return this.previewQuestions[Math.min(this.phase, this.previewQuestions.length - 1)] ?? null;
    }
    return this.caseDetail.currentQuestion;
  }

  get progressLabel(): string {
    const total = this.caseDetail.totalQuestions || 1;
    return `Decisión ${Math.min(this.phase + 1, total)} / ${total}`;
  }

  get progressPercent(): number {
    const total = this.caseDetail.totalQuestions || 1;
    return Math.round((this.phase / total) * 100);
  }

  get timerLabel(): string {
    const m = Math.floor(this.elapsedSeconds / 60);
    const s = this.elapsedSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  get categoryLabel(): string {
    return scoreCategoryLabel(this.lastFeedback?.category || '');
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    const k = e.key.toLowerCase();
    if (k === 'f') { e.preventDefault(); this.toggleFullscreen(); return; }
    if (this.gamePhase !== 'playing') return;
    if (['w', 'a', 's', 'd', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(k)) {
      this.keys.add(k);
      this.moveHint = false;
      e.preventDefault();
    }
    if (k === 'e' || k === 'enter') this.openDecision();
  }

  @HostListener('window:keyup', ['$event'])
  onKeyUp(e: KeyboardEvent): void { this.keys.delete(e.key.toLowerCase()); }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void { this.isFullscreen = !!document.fullscreenElement; }

  move(dir: 'up' | 'down' | 'left' | 'right'): void {
    if (this.gamePhase !== 'playing') return;
    const map: Record<string, string> = { up: 'w', down: 's', left: 'a', right: 'd' };
    this.keys.add(map[dir]);
    this.moveHint = false;
    setTimeout(() => this.keys.delete(map[dir]), 150);
  }

  startGame(): void {
    if (!this.currentQuestion) {
      this.toast.error('Este caso no tiene preguntas configuradas.');
      return;
    }
    this.gamePhase = 'playing';
    this.moveHint = true;
    if (!this.previewMode) this.startTimer();
  }

  openDecision(): void {
    if (!this.currentQuestion?.options?.length) {
      this.toast.error('No hay opciones de respuesta.');
      return;
    }
    this.gamePhase = 'decision';
  }

  closeDecision(): void {
    if (!this.submitting) this.gamePhase = 'playing';
  }

  get sortedOptions() {
    return [...(this.currentQuestion?.options ?? [])].sort((a, b) => a.orderIndex - b.orderIndex);
  }

  toggleFullscreen(): void {
    const el = this.gameRootRef.nativeElement;
    if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => this.toast.error('No se pudo activar pantalla completa'));
    else document.exitFullscreen?.();
  }

  exitGame(): void {
    this.stopTimer();
    if (document.fullscreenElement) document.exitFullscreen?.();
    this.closed.emit();
  }

  selectOption(optionId: number): void {
    if (!this.currentQuestion || this.submitting || this.previewMode) return;
    this.submitting = true;
    this.api.submitAnswer(this.caseDetail.id, this.currentQuestion.id, optionId, this.elapsedSeconds).subscribe({
      next: (res) => {
        this.submitting = false;
        this.caseDetail = res.caseDetail;
        this.caseUpdated.emit(res.caseDetail);
        this.afterAnswer(res);
      },
      error: (err) => {
        this.submitting = false;
        this.toast.error(err.error?.message || 'Error al registrar la decisión');
      },
    });
  }

  selectPreviewOption(): void {
    if (!this.previewMode || !this.currentQuestion) return;
    this.lastFeedback = { correct: true, message: 'Vista previa: sin puntaje.', category: 'CLINICAL', correctAnswerText: null };
    this.gamePhase = 'feedback';
  }

  continueAfterFeedback(): void {
    if (this.previewMode) {
      const next = this.phase + 1;
      if (next >= (this.previewQuestions.length || this.caseDetail.totalQuestions)) {
        this.gamePhase = 'finished';
        this.resultMessage = 'Vista previa completada. Ningún puntaje fue guardado.';
        return;
      }
      this.phase = next;
      this.gamePhase = 'transition';
      setTimeout(() => { this.loadScene(); this.gamePhase = 'playing'; }, 900);
      return;
    }
    if (this.attemptResult) {
      this.gamePhase = 'finished';
      this.resultPassed = this.attemptResult.passed;
      this.resultMessage = this.resultPassed
        ? `¡Aprobaste con ${this.attemptResult.totalScore}%!`
        : this.attemptResult.passed === false && this.caseDetail.studentStatus === 'BLOCKED'
          ? 'Intentos agotados. Solicita reinicio al profesor.'
          : `Obtuviste ${this.attemptResult.totalScore}%. Puedes reintentar si tienes intentos.`;
      this.stopTimer();
      return;
    }
    this.phase = this.caseDetail.currentQuestionIndex;
    this.gamePhase = 'transition';
    setTimeout(() => { this.loadScene(); this.gamePhase = 'playing'; }, 900);
  }

  private afterAnswer(res: { feedback: AnswerFeedback; result: AttemptResult | null; caseDetail: CaseDetail }): void {
    this.lastFeedback = res.feedback;
    this.attemptResult = res.result;
    this.gamePhase = 'feedback';
    if (res.result) this.stopTimer();
  }

  private buildScene(index: number): SceneConfig {
    const q = this.currentQuestion;
    const palette = SCENE_PALETTES[index % SCENE_PALETTES.length];
    const targetX = 620 + (index % 3) * 40;
    const targetY = 280 + (index % 2) * 30;
    const npcLabel = q?.npcLabel || 'Persona';

    return {
      title: q?.sceneTitle || `Escena ${index + 1}`,
      subtitle: q?.sceneSubtitle || this.caseDetail.category || 'Simulación',
      hint: q?.sceneHint || 'Acércate al punto de interés y responde la decisión.',
      floorColor: palette.floor,
      accentColor: palette.accent,
      playerStart: { x: 380 - (index % 2) * 60, y: 340 },
      target: { x: targetX, y: targetY, radius: 90, label: 'Tomar decisión' },
      walls: DEFAULT_WALLS,
      npcs: npcLabel ? [{ id: 'npc', x: targetX, y: targetY, color: '#ff8a8a', label: npcLabel, pulse: true }] : [],
      props: [
        { x: 180, y: 80, w: 120, h: 60, color: '#1e4a55', label: 'Entrada' },
        { x: 420, y: 160, w: 200, h: 140, color: '#163840', label: 'Zona central' },
        { x: 760, y: 200, w: 90, h: 50, color: '#2a5560', label: 'Salida' },
      ],
    };
  }

  private startTimer(): void {
    if (!this.caseDetail.timerEnabled || this.previewMode) return;
    this.stopTimer();
    this.timerInterval = setInterval(() => { this.elapsedSeconds++; }, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) { clearInterval(this.timerInterval); this.timerInterval = null; }
  }

  private loadScene(): void {
    this.player = { ...this.currentScene.playerStart };
    this.loadSceneImage();
  }

  private loadSceneImage(): void {
    const url = this.currentQuestion?.sceneImageUrl?.trim();
    if (!url) {
      this.sceneImage = null;
      this.sceneImageUrl = '';
      return;
    }
    if (url === this.sceneImageUrl) return;

    this.sceneImageUrl = url;
    this.sceneImage = null;
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      if (this.sceneImageUrl === url) this.sceneImage = img;
    };
    img.onerror = () => {
      if (this.sceneImageUrl === url) this.sceneImage = null;
    };
    img.src = url;
  }

  private loop(): void {
    this.pulse += 0.04;
    this.update();
    this.draw();
    this.animId = requestAnimationFrame(() => this.loop());
  }

  private update(): void {
    if (this.gamePhase !== 'playing') return;
    const speed = 3.2;
    let dx = 0, dy = 0;
    if (this.keys.has('w') || this.keys.has('arrowup')) dy -= speed;
    if (this.keys.has('s') || this.keys.has('arrowdown')) dy += speed;
    if (this.keys.has('a') || this.keys.has('arrowleft')) dx -= speed;
    if (this.keys.has('d') || this.keys.has('arrowright')) dx += speed;
    if (dx !== 0 || dy !== 0) {
      const len = Math.hypot(dx, dy) || 1;
      dx = (dx / len) * speed;
      dy = (dy / len) * speed;
      const next = { x: this.player.x + dx, y: this.player.y + dy };
      if (!this.collides(next)) this.player = next;
    }
  }

  private collides(pos: Vec2): boolean {
    const r = this.playerRadius - 2;
    for (const w of this.currentScene.walls) {
      if (pos.x - r < w.x + w.w && pos.x + r > w.x && pos.y - r < w.y + w.h && pos.y + r > w.y) return true;
    }
    return pos.x < 50 || pos.x > this.width - 50 || pos.y < 50 || pos.y > this.height - 50;
  }

  private draw(): void {
    const ctx = this.ctx;
    const scene = this.currentScene;
    ctx.clearRect(0, 0, this.width, this.height);

    if (this.sceneImage) {
      ctx.drawImage(this.sceneImage, 0, 0, this.width, this.height);
      ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
      ctx.fillRect(0, 0, this.width, this.height);
    } else {
      const grd = ctx.createLinearGradient(0, 0, 0, this.height);
      grd.addColorStop(0, scene.floorColor);
      grd.addColorStop(1, '#081a20');
      ctx.fillStyle = grd;
      ctx.fillRect(0, 0, this.width, this.height);
    }

    ctx.strokeStyle = 'rgba(94, 234, 255, 0.06)';
    for (let x = 0; x < this.width; x += 40) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, this.height); ctx.stroke();
    }
    for (let y = 0; y < this.height; y += 40) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(this.width, y); ctx.stroke();
    }

    for (const p of scene.props) {
      ctx.fillStyle = p.color;
      ctx.strokeStyle = 'rgba(94, 234, 255, 0.35)';
      ctx.fillRect(p.x, p.y, p.w, p.h);
      ctx.strokeRect(p.x, p.y, p.w, p.h);
      if (p.label) {
        ctx.fillStyle = 'rgba(255,255,255,0.75)';
        ctx.font = '11px Inter, sans-serif';
        ctx.fillText(p.label, p.x + 8, p.y + 18);
      }
    }

    ctx.fillStyle = 'rgba(0,0,0,0.45)';
    for (const w of scene.walls) ctx.fillRect(w.x, w.y, w.w, w.h);

    const t = scene.target;
    const pulseR = t.radius + Math.sin(this.pulse) * 8;
    ctx.beginPath();
    ctx.arc(t.x, t.y, pulseR, 0, Math.PI * 2);
    ctx.strokeStyle = scene.accentColor + '88';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = scene.accentColor + '18';
    ctx.fill();

    for (const npc of scene.npcs) {
      const pr = 16 + (npc.pulse ? Math.sin(this.pulse * 2) * 3 : 0);
      ctx.beginPath();
      ctx.arc(npc.x, npc.y, pr, 0, Math.PI * 2);
      ctx.fillStyle = npc.color;
      ctx.fill();
      ctx.strokeStyle = '#ffffffaa';
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = '#fff';
      ctx.font = '11px Inter, sans-serif';
      ctx.fillText(npc.label, npc.x - 20, npc.y - pr - 6);
    }

    ctx.beginPath();
    ctx.arc(this.player.x, this.player.y, this.playerRadius, 0, Math.PI * 2);
    ctx.fillStyle = '#5eeaff';
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.fillStyle = '#0b3a42';
    ctx.font = 'bold 10px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('TÚ', this.player.x, this.player.y + 4);
    ctx.textAlign = 'left';
  }
}
