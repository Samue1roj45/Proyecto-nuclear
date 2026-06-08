import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthTransitionService } from '../../services/auth-transition.service';
import { OAuthService } from '../../services/oauth.service';

type LoginView = 'login' | 'forgot-request' | 'forgot-reset' | 'register';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private authTransition = inject(AuthTransitionService);
  oauth = inject(OAuthService);

  view: LoginView = 'login';

  email = '';
  password = '';
  showPassword = false;
  loading = false;
  transitioning = false;
  welcomeName = '';
  error = '';
  success = '';
  oauthReady = false;

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
  }

  login(): void {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.enterApp(),
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Credenciales inválidas';
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
          next: () => this.enterApp(),
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
          next: () => this.enterApp(),
          error: (err) => this.handleOAuthError(err),
        });
      })
      .catch((err) => this.handleOAuthError(err));
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
        this.success = res.message;
        this.view = 'login';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'No se pudo crear la cuenta';
      },
    });
  }

  backToLogin(): void {
    this.view = 'login';
    this.error = '';
    this.success = '';
  }
}
