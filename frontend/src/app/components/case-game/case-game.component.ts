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
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { CaseDetail, Question } from '../../models';

interface Vec2 {
  x: number;
  y: number;
}

interface Wall {
  x: number;
  y: number;
  w: number;
  h: number;
}

interface Npc {
  id: string;
  x: number;
  y: number;
  color: string;
  label: string;
  pulse?: boolean;
}

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

const SCENES: SceneConfig[] = [
  {
    title: 'Escena 1 · Llegada de emergencia',
    subtitle: 'Unidad residencial · Noche',
    hint: 'Muévete con WASD o flechas. Acércate a la víctima.',
    floorColor: '#0f2d35',
    accentColor: '#00d4f0',
    playerStart: { x: 520, y: 300 },
    target: { x: 650, y: 290, radius: 90, label: 'Evaluar víctima' },
    walls: [
      { x: 0, y: 0, w: 960, h: 40 },
      { x: 0, y: 500, w: 960, h: 40 },
      { x: 0, y: 0, w: 40, h: 540 },
      { x: 920, y: 0, w: 40, h: 540 },
    ],
    npcs: [{ id: 'victim', x: 650, y: 290, color: '#ff6b7a', label: 'Víctima', pulse: true }],
    props: [
      { x: 180, y: 80, w: 120, h: 60, color: '#1e4a55', label: 'Ambulancia' },
      { x: 420, y: 160, w: 200, h: 140, color: '#163840', label: 'Unidad' },
      { x: 760, y: 200, w: 90, h: 50, color: '#2a5560', label: 'Balcón' },
    ],
  },
  {
    title: 'Escena 2 · Sala de contención',
    subtitle: 'Interior · Zona segura',
    hint: 'La víctima muestra miedo extremo. Acércate con cuidado.',
    floorColor: '#122830',
    accentColor: '#5eeaff',
    playerStart: { x: 380, y: 340 },
    target: { x: 500, y: 300, radius: 90, label: 'Contener emocionalmente' },
    walls: [
      { x: 0, y: 0, w: 960, h: 36 },
      { x: 0, y: 504, w: 960, h: 36 },
      { x: 0, y: 0, w: 36, h: 540 },
      { x: 924, y: 0, w: 36, h: 540 },
    ],
    npcs: [{ id: 'victim', x: 500, y: 300, color: '#ff8a8a', label: 'Víctima', pulse: true }],
    props: [
      { x: 260, y: 320, w: 140, h: 70, color: '#1a3d47', label: 'Sofá' },
      { x: 620, y: 200, w: 80, h: 120, color: '#1f4550', label: 'Ventana' },
      { x: 720, y: 360, w: 100, h: 60, color: '#254a54', label: 'Botiquín' },
    ],
  },
  {
    title: 'Escena 3 · Activación de rutas',
    subtitle: 'Puesto de coordinación',
    hint: 'Ve al panel de rutas institucionales para tomar la decisión final.',
    floorColor: '#0d2630',
    accentColor: '#ffd166',
    playerStart: { x: 580, y: 320 },
    target: { x: 720, y: 250, radius: 90, label: 'Panel de rutas VBG' },
    walls: [
      { x: 0, y: 0, w: 960, h: 36 },
      { x: 0, y: 504, w: 960, h: 36 },
      { x: 0, y: 0, w: 36, h: 540 },
      { x: 924, y: 0, w: 36, h: 540 },
    ],
    npcs: [],
    props: [
      { x: 640, y: 180, w: 160, h: 140, color: '#1c4452', label: 'Rutas VBG' },
      { x: 200, y: 160, w: 200, h: 100, color: '#173a44', label: 'Teléfono 123' },
      { x: 420, y: 400, w: 120, h: 50, color: '#1e4a55', label: 'Comisaría' },
    ],
  },
];

@Component({
  selector: 'app-case-game',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-game.component.html',
  styleUrl: './case-game.component.scss',
})
export class CaseGameComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) caseDetail!: CaseDetail;
  @Input() previewMode = false;
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
  submitting = false;

  gamePhase: 'intro' | 'playing' | 'decision' | 'transition' | 'finished' = 'intro';
  isFullscreen = false;
  showHud = true;
  moveHint = true;
  resultMessage = '';
  resultPassed = false;

  readonly width = 960;
  readonly height = 540;
  readonly passThreshold = 60;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseDetail']?.currentValue) {
      const detail = changes['caseDetail'].currentValue as CaseDetail;
      this.phase = detail.currentQuestionIndex ?? this.phase;
    }
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    this.phase = this.caseDetail.currentQuestionIndex ?? 0;
    if (this.caseDetail.studentStatus === 'PASSED' || this.caseDetail.studentStatus === 'FAILED' || this.caseDetail.studentStatus === 'BLOCKED') {
      if (!this.caseDetail.currentQuestion) {
        this.gamePhase = 'finished';
        this.resultPassed = this.caseDetail.studentStatus === 'PASSED';
        this.resultMessage = this.resultPassed
          ? 'Completaste el caso. Revisa tu puntaje en Reportes.'
          : 'Intento finalizado. Consulta tus resultados en Reportes.';
      }
    } else if (this.caseDetail.activeAttemptId && this.caseDetail.currentQuestion) {
      this.gamePhase = 'playing';
      this.moveHint = false;
    }
    this.loadScene();
    this.loop();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animId);
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => undefined);
    }
  }

  get currentScene(): SceneConfig {
    return SCENES[Math.min(this.phase, SCENES.length - 1)];
  }

  get currentQuestion(): Question | null {
    return this.caseDetail.currentQuestion;
  }

  get progressLabel(): string {
    const total = this.caseDetail.totalQuestions || 3;
    return `Decisión ${Math.min(this.phase + 1, total)} / ${total}`;
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    const k = e.key.toLowerCase();
    if (k === 'f') {
      e.preventDefault();
      this.toggleFullscreen();
      return;
    }
    if (this.gamePhase !== 'playing') return;
    if (['w', 'a', 's', 'd', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(k)) {
      this.keys.add(k);
      this.moveHint = false;
      e.preventDefault();
    }
    if (k === 'e' || k === 'enter') this.openDecision();
  }

  @HostListener('window:keyup', ['$event'])
  onKeyUp(e: KeyboardEvent): void {
    this.keys.delete(e.key.toLowerCase());
  }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    this.isFullscreen = !!document.fullscreenElement;
  }

  startGame(): void {
    if (!this.currentQuestion) {
      this.toast.error('Este caso no tiene preguntas configuradas para el simulador.');
      return;
    }
    this.gamePhase = 'playing';
    this.moveHint = true;
  }

  openDecision(): void {
    if (!this.currentQuestion) {
      this.toast.error('No hay pregunta activa en este momento.');
      return;
    }
    if (!this.currentQuestion.options?.length) {
      this.toast.error('La pregunta actual no tiene opciones de respuesta.');
      return;
    }
    this.gamePhase = 'decision';
  }

  closeDecision(): void {
    if (!this.submitting) this.gamePhase = 'playing';
  }

  get nearTarget(): boolean {
    const t = this.currentScene.target;
    return Math.hypot(this.player.x - t.x, this.player.y - t.y) <= t.radius + 20;
  }

  get sortedOptions() {
    const opts = this.currentQuestion?.options ?? [];
    return [...opts].sort((a, b) => a.orderIndex - b.orderIndex);
  }

  toggleFullscreen(): void {
    const el = this.gameRootRef.nativeElement;
    if (!document.fullscreenElement) {
      el.requestFullscreen?.().catch(() => this.toast.error('No se pudo activar pantalla completa'));
    } else {
      document.exitFullscreen?.();
    }
  }

  exitGame(): void {
    if (document.fullscreenElement) document.exitFullscreen?.();
    this.closed.emit();
  }

  selectOption(optionId: number): void {
    if (!this.currentQuestion || this.submitting || this.previewMode) return;
    this.submitting = true;
    this.api.submitAnswer(this.caseDetail.id, this.currentQuestion.id, optionId).subscribe({
      next: (updated) => {
        this.submitting = false;
        this.caseDetail = updated;
        this.caseUpdated.emit(updated);
        this.afterAnswer(updated);
      },
      error: (err) => {
        this.submitting = false;
        this.toast.error(err.error?.message || 'Error al registrar la decisión');
      },
    });
  }

  selectPreviewOption(): void {
    if (!this.previewMode || !this.currentQuestion) return;
    this.phase++;
    if (this.phase >= SCENES.length || this.phase >= this.caseDetail.totalQuestions) {
      this.gamePhase = 'finished';
      this.resultMessage = 'Modo vista previa completado.';
      return;
    }
    this.gamePhase = 'transition';
    setTimeout(() => {
      this.loadScene();
      this.gamePhase = 'playing';
    }, 900);
  }

  private afterAnswer(updated: CaseDetail): void {
    if (updated.studentStatus === 'PASSED' || updated.studentStatus === 'FAILED' || updated.studentStatus === 'BLOCKED') {
      this.gamePhase = 'finished';
      this.resultPassed = updated.studentStatus === 'PASSED';
      this.resultMessage = this.resultPassed
        ? `¡Aprobaste con éxito! Superaste el ${this.passThreshold}% requerido.`
        : updated.studentStatus === 'BLOCKED'
          ? 'Intentos agotados. Solicita reinicio al profesor.'
          : `No alcanzaste el ${this.passThreshold}% mínimo. Aún puedes reintentar si tienes intentos.`;
      return;
    }
    this.phase = updated.currentQuestionIndex;
    this.gamePhase = 'transition';
    setTimeout(() => {
      this.loadScene();
      this.gamePhase = 'playing';
    }, 1000);
  }

  private loadScene(): void {
    const scene = this.currentScene;
    this.player = { ...scene.playerStart };
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
    let dx = 0;
    let dy = 0;
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
      if (pos.x - r < w.x + w.w && pos.x + r > w.x && pos.y - r < w.y + w.h && pos.y + r > w.y) {
        return true;
      }
    }
    return pos.x < 50 || pos.x > this.width - 50 || pos.y < 50 || pos.y > this.height - 50;
  }

  private draw(): void {
    const ctx = this.ctx;
    const scene = this.currentScene;
    ctx.clearRect(0, 0, this.width, this.height);

    // Suelo
    const grd = ctx.createLinearGradient(0, 0, 0, this.height);
    grd.addColorStop(0, scene.floorColor);
    grd.addColorStop(1, '#081a20');
    ctx.fillStyle = grd;
    ctx.fillRect(0, 0, this.width, this.height);

    // Grid sutil
    ctx.strokeStyle = 'rgba(94, 234, 255, 0.06)';
    ctx.lineWidth = 1;
    for (let x = 0; x < this.width; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, this.height);
      ctx.stroke();
    }
    for (let y = 0; y < this.height; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(this.width, y);
      ctx.stroke();
    }

    // Props
    for (const p of scene.props) {
      ctx.fillStyle = p.color;
      ctx.strokeStyle = 'rgba(94, 234, 255, 0.35)';
      ctx.lineWidth = 2;
      ctx.fillRect(p.x, p.y, p.w, p.h);
      ctx.strokeRect(p.x, p.y, p.w, p.h);
      if (p.label) {
        ctx.fillStyle = 'rgba(255,255,255,0.75)';
        ctx.font = '11px Inter, sans-serif';
        ctx.fillText(p.label, p.x + 8, p.y + 18);
      }
    }

    // Paredes
    ctx.fillStyle = 'rgba(0,0,0,0.45)';
    for (const w of scene.walls) ctx.fillRect(w.x, w.y, w.w, w.h);

    // Zona objetivo
    const t = scene.target;
    const pulseR = t.radius + Math.sin(this.pulse) * 8;
    ctx.beginPath();
    ctx.arc(t.x, t.y, pulseR, 0, Math.PI * 2);
    ctx.strokeStyle = scene.accentColor + '88';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = scene.accentColor + '18';
    ctx.fill();

    ctx.fillStyle = '#ffffff';
    ctx.font = '600 12px Manrope, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(t.label, t.x, t.y - pulseR - 8);
    ctx.textAlign = 'left';

    // NPCs
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

    // Jugador (psicólogo)
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

    // Viñeta
    const v = ctx.createRadialGradient(this.width / 2, this.height / 2, 200, this.width / 2, this.height / 2, 520);
    v.addColorStop(0, 'rgba(0,0,0,0)');
    v.addColorStop(1, 'rgba(0,0,0,0.45)');
    ctx.fillStyle = v;
    ctx.fillRect(0, 0, this.width, this.height);
  }
}
