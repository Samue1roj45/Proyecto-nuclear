import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import QRCode from 'qrcode';

interface FlowStep {
  icon: string;
  title: string;
  description: string;
}

interface NetworkUrlConfig {
  baseUrl: string;
  infoUrl: string;
}

@Component({
  selector: 'app-platform-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './platform-info.component.html',
  styleUrl: './platform-info.component.scss',
})
export class PlatformInfoComponent implements OnInit {
  @ViewChild('qrCanvas') qrCanvas?: ElementRef<HTMLCanvasElement>;

  infoUrl = '';
  qrReady = false;
  loadingQr = true;
  usesLanUrl = false;
  showQrOnly = false;
  showGuideOnly = false;

  readonly studentSteps: FlowStep[] = [
    {
      icon: 'school',
      title: 'Conoce el simulador',
      description: 'Lee como funciona el proceso academico antes de usar la aplicacion en clase.',
    },
    {
      icon: 'person_add',
      title: 'Registro e ingreso',
      description: 'El estudiante se registra o inicia sesion con las credenciales del docente.',
    },
    {
      icon: 'hourglass_top',
      title: 'Aprobacion del docente',
      description: 'El profesor autoriza el acceso desde el panel de solicitudes.',
    },
    {
      icon: 'pin',
      title: 'Codigo de acceso',
      description: 'El estudiante ingresa el codigo entregado para completar el acceso.',
    },
    {
      icon: 'dashboard',
      title: 'Dashboard de casos',
      description: 'Se elige un caso clinico y se revisa el estado de cada intento.',
    },
    {
      icon: 'psychology',
      title: 'Simulador interactivo',
      description: 'Se responden preguntas del caso con retroalimentacion inmediata.',
    },
    {
      icon: 'assessment',
      title: 'Reportes de desempeno',
      description: 'Se consultan puntajes clinicos, eticos y normativos de cada intento.',
    },
  ];

  readonly teacherSteps: FlowStep[] = [
    {
      icon: 'admin_panel_settings',
      title: 'Panel administrativo',
      description: 'El docente ingresa con rol administrador.',
    },
    {
      icon: 'how_to_reg',
      title: 'Solicitudes de acceso',
      description: 'Aprueba o rechaza estudiantes que quieren entrar.',
    },
    {
      icon: 'edit_note',
      title: 'Gestion de casos',
      description: 'Crea y edita casos, preguntas y respuestas.',
    },
    {
      icon: 'groups',
      title: 'Grupos y seguimiento',
      description: 'Organiza estudiantes y revisa su progreso.',
    },
  ];

  readonly features = [
    { icon: 'school', label: 'Simulacion academica de psicologia social' },
    { icon: 'verified', label: 'Evaluacion clinica, etica y normativa' },
    { icon: 'timeline', label: 'Historial de intentos y reportes' },
    { icon: 'notifications', label: 'Notificaciones y reinicios de casos' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    const onLocalhost = /localhost|127\.0\.0\.1/i.test(window.location.hostname);
    this.showQrOnly = onLocalhost;
    this.showGuideOnly = !onLocalhost;

    if (!this.showQrOnly) {
      this.loadingQr = false;
      return;
    }

    this.resolveInfoUrl().then((url) => {
      this.infoUrl = url;
      this.loadingQr = false;
      setTimeout(() => this.renderQr());
    });
  }

  async downloadQr(): Promise<void> {
    if (!this.infoUrl) return;
    const dataUrl = await QRCode.toDataURL(this.infoUrl, {
      width: 900,
      margin: 2,
      color: { dark: '#006474', light: '#f8fafb' },
    });
    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = 'mision-psicosocial-guia-qr.png';
    link.click();
  }

  private async resolveInfoUrl(): Promise<string> {
    try {
      const config = await firstValueFrom(
        this.http.get<NetworkUrlConfig>(`/network-url.json?v=${Date.now()}`)
      );
      if (config?.infoUrl) {
        this.usesLanUrl = true;
        return config.infoUrl;
      }
      if (config?.baseUrl) {
        this.usesLanUrl = true;
        return `${config.baseUrl}/info`;
      }
    } catch {
      /* fallback below */
    }

    return `${window.location.origin}/info`;
  }

  private async renderQr(): Promise<void> {
    const canvas = this.qrCanvas?.nativeElement;
    if (!canvas || !this.infoUrl) return;
    await QRCode.toCanvas(canvas, this.infoUrl, {
      width: 320,
      margin: 1,
      color: { dark: '#006474', light: '#f8fafb' },
    });
    this.qrReady = true;
  }
}
