import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AccessFlowService } from '../../services/access-flow.service';
import { AuthTransitionService } from '../../services/auth-transition.service';
import { OAuthService } from '../../services/oauth.service';
import { AccessFlowState, LoginStepResponse } from '../../models/access-flow';

type LoginView =
  | 'login'
  | 'access-pending'
  | 'access-code'
  | 'access-denied'
  | 'forgot-request'
  | 'forgot-reset'
  | 'register';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private accessFlow = inject(AccessFlowService);
  private router = inject(Router);
  private authTransition = inject(AuthTransitionService);
  oauth = inject(OAuthService);

  view: LoginView = 'login';

  email = '';
  password = '';
  accessCode = '';
  accessSession = '';
  studentName = '';
  showPassword = false;
  loading = false;
  transitioning = false;
  welcomeName = '';
  error = '';
  success = '';
  detail = '';
  oauthReady = false;
  expiresInMinutes = 10;
  expiresAt = '';
  emailSent = false;
  checkingStatus = false;

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  registerName = '';
  registerEmail = '';
  registerPassword = '';
  registerConfirm = '';
  showRegisterPassword = false;

  resetCode = '';
  newPassword = '';
  confirmPassword = '';
  showNewPassword = false;

  ngOnInit(): void {
    if (this.oauth.isAvailable) {
      this.oauth.init().then(() => (this.oauthReady = true));
    }
    this.resumeSavedFlow();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  get currentStep(): number {
    if (this.view === 'login') return 1;
    if (this.view === 'access-pending') return 2;
    if (this.view === 'access-code') return 3;
    return 1;
  }

  get codeHint(): string {
    if (this.emailSent) {
      return 'Revisa tu correo (incluida la carpeta de spam). El código tiene 6 dígitos.';
    }
    return 'Tu docente te entregará el código. Escríbelo aquí cuando lo recibas.';
  }

  login(): void {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.detail = '';
    this.auth.login(this.email, this.password).subscribe({
      next: (res) => this.handleLoginStep(res),
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Credenciales inválidas';
      },
    });
  }

  verifyAccessCode(): void {
    const code = this.accessCode.trim();
    if (!/^\d{6}$/.test(code)) {
      this.error = 'Ingresa un código válido de 6 dígitos';
      return;
    }
    if (!this.accessSession) {
      this.error = 'Sesión expirada. Vuelve a iniciar sesión.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.auth.verifyAccessCode(this.email, code, this.accessSession).subscribe({
      next: () => {
        this.loading = false;
        this.stopPolling();
        this.accessFlow.clear();
        this.enterApp();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Código inválido';
      },
    });
  }

  signInWithGoogle(): void {
    this.loading = true;
    this.error = '';
    this.oauth
      .signInWithGoogle()
      .then((code) => {
        this.auth.oauthGoogle(code).subscribe({
          next: (res) => this.handleLoginStep(res),
          error: (err) => this.handleOAuthError(err),
        });
      })
      .catch((err) => this.handleOAuthError(err));
  }

  signInWithFacebook(): void {
    this.loading = true;
    this.error = '';
    this.oauth
      .signInWithFacebook()
      .then((token) => {
        this.auth.oauthFacebook(token).subscribe({
          next: (res) => this.handleLoginStep(res),
          error: (err) => this.handleOAuthError(err),
        });
      })
      .catch((err) => this.handleOAuthError(err));
  }

  private handleLoginStep(res: LoginStepResponse): void {
    this.loading = false;
    this.email = res.email || this.email;
    this.studentName = res.studentName || this.studentName;
    this.accessSession = res.accessSession || '';
    this.success = res.message;
    this.detail = res.detail || '';
    this.expiresInMinutes = res.expiresInMinutes ?? this.expiresInMinutes;
    this.expiresAt = res.expiresAt || '';
    this.emailSent = !!res.emailSent;

    if (res.step === 'DIRECT_ACCESS' && res.auth) {
      this.auth.storeAuth(res.auth);
      this.accessFlow.clear();
      this.enterApp();
      return;
    }

    if (!res.accessSession) {
      this.error = res.message || 'No se pudo iniciar el flujo de acceso.';
      return;
    }

    this.accessFlow.saveFromLogin(res);

    if (res.step === 'CODE_REQUIRED') {
      this.view = 'access-code';
      this.stopPolling();
      return;
    }

    if (res.step === 'REQUEST_SUBMITTED' || res.step === 'AWAITING_APPROVAL') {
      this.view = 'access-pending';
      this.startPolling();
      return;
    }

    this.error = res.message || 'No se pudo completar el inicio de sesión.';
  }

  private resumeSavedFlow(): void {
    const saved = this.accessFlow.load();
    if (!saved) return;

    this.email = saved.email;
    this.studentName = saved.studentName;
    this.accessSession = saved.accessSession;
    this.view = saved.step;
    this.expiresInMinutes = saved.expiresInMinutes ?? 10;
    this.expiresAt = saved.expiresAt || '';
    this.emailSent = !!saved.emailSent;

    if (saved.step === 'access-pending') {
      this.success = 'Paso 2 de 3: esperando al docente';
      this.startPolling();
    } else {
      this.success = 'Paso 3 de 3: ingresa tu código';
      this.detail = this.codeHint;
    }

    this.checkAccessStatus(false);
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollTimer = setInterval(() => this.checkAccessStatus(false), 4000);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  checkAccessStatus(showFeedback = true): void {
    if (!this.email || !this.accessSession) return;
    this.checkingStatus = showFeedback;

    this.auth.getAccessStatus(this.accessSession, this.email).subscribe({
      next: (status) => {
        this.checkingStatus = false;
        this.success = status.message;
        this.detail = status.detail || '';
        this.studentName = status.studentName || this.studentName;
        this.expiresInMinutes = status.expiresInMinutes ?? this.expiresInMinutes;
        this.expiresAt = status.expiresAt || this.expiresAt;

        if (status.status === 'RECHAZADO' || status.status === 'EXPIRADO') {
          this.stopPolling();
          this.view = 'access-denied';
          this.error = '';
          this.success = status.message;
          this.detail = status.detail || '';
          this.accessFlow.clear();
          return;
        }

        if (status.canEnterCode) {
          this.stopPolling();
          this.view = 'access-code';
          this.error = '';
          this.accessFlow.save({
            email: this.email,
            studentName: this.studentName,
            accessSession: this.accessSession,
            step: 'access-code',
            expiresInMinutes: this.expiresInMinutes,
            expiresAt: this.expiresAt,
            emailSent: this.emailSent,
          });
        }
      },
      error: (err) => {
        this.checkingStatus = false;
        if (showFeedback) {
          this.error = err.error?.message || 'No se pudo verificar el estado';
        }
        if (err.status === 400) {
          this.stopPolling();
          this.accessFlow.clear();
        }
      },
    });
  }

  backToCredentials(): void {
    this.stopPolling();
    this.accessFlow.clear();
    this.view = 'login';
    this.error = '';
    this.success = '';
    this.detail = '';
    this.accessCode = '';
    this.accessSession = '';
    this.password = '';
  }

  retryAccessRequest(): void {
    this.backToCredentials();
    this.success = 'Inicia sesión nuevamente para enviar una nueva solicitud de acceso.';
  }

  onCodeInput(): void {
    this.accessCode = this.accessCode.replace(/\D/g, '').slice(0, 6);
  }

  private handleOAuthError(err: { error?: { message?: string }; message?: string }): void {
    this.loading = false;
    this.error = err.error?.message || err.message || 'No se pudo iniciar con la cuenta social';
  }

  private enterApp(): void {
    this.welcomeName = this.auth.currentUser()?.fullName || 'Usuario';
    this.transitioning = true;
    setTimeout(() => {
      this.authTransition.startEnter();
      this.router.navigateByUrl(this.auth.homeRoute());
    }, 1600);
  }

  openForgot(): void {
    this.view = 'forgot-request';
    this.error = '';
    this.success = '';
    this.resetCode = '';
    this.newPassword = '';
    this.confirmPassword = '';
  }

  requestResetCode(): void {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.auth.forgotPassword(this.email).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
        this.view = 'forgot-reset';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'No se pudo enviar el correo';
      },
    });
  }

  resetPassword(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.error = 'Las contraseñas no coinciden';
      return;
    }
    if (this.newPassword.length < 6) {
      this.error = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';
    this.auth.resetPassword(this.email, this.resetCode, this.newPassword).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
        this.password = '';
        this.view = 'login';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'No se pudo restablecer la contraseña';
      },
    });
  }

  openRegister(): void {
    this.view = 'register';
    this.error = '';
    this.success = '';
    this.registerName = '';
    this.registerEmail = '';
    this.registerPassword = '';
    this.registerConfirm = '';
  }

  register(): void {
    if (this.registerPassword !== this.registerConfirm) {
      this.error = 'Las contraseñas no coinciden';
      return;
    }
    if (this.registerPassword.length < 6) {
      this.error = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';
    this.auth.register(this.registerName, this.registerEmail, this.registerPassword).subscribe({
      next: (res) => {
        this.loading = false;
        this.email = this.registerEmail;
        this.password = '';
        this.success =
          res.message +
          ' Al iniciar sesión, el docente deberá aprobar tu acceso y recibirás un código temporal.';
        this.view = 'login';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'No se pudo crear la cuenta';
      },
    });
  }

  backToLogin(): void {
    this.stopPolling();
    this.view = 'login';
    this.error = '';
    this.success = '';
  }
}
