import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

declare const FB: {
  init(params: Record<string, unknown>): void;
  login(
    callback: (response: { authResponse?: { accessToken: string } }) => void,
    options: { scope: string }
  ): void;
};

@Injectable({ providedIn: 'root' })
export class OAuthService {
  readonly hasGoogle = !!environment.googleClientId;
  readonly hasFacebook = !!environment.facebookAppId;

  private googleReady = false;
  private facebookReady = false;

  get isAvailable(): boolean {
    return this.hasGoogle || this.hasFacebook;
  }

  async init(): Promise<void> {
    const tasks: Promise<void>[] = [];
    if (this.hasGoogle) tasks.push(this.loadGoogle());
    if (this.hasFacebook) tasks.push(this.loadFacebook());
    await Promise.all(tasks);
  }

  signInWithGoogle(): Promise<string> {
    if (!this.hasGoogle || !this.googleReady) {
      return Promise.reject(new Error('Google Sign-In no está configurado'));
    }

    const google = (window as unknown as { google: { accounts: { oauth2: { initCodeClient: Function } } } }).google;

    return new Promise((resolve, reject) => {
      try {
        const client = google.accounts.oauth2.initCodeClient({
          client_id: environment.googleClientId,
          scope: 'openid email profile',
          ux_mode: 'popup',
          callback: (response: { code?: string; error?: string }) => {
            if (response.code) {
              resolve(response.code);
            } else {
              reject(new Error(response.error || 'Inicio con Google cancelado'));
            }
          },
        });
        client.requestCode();
      } catch {
        reject(new Error('No se pudo abrir Google Sign-In'));
      }
    });
  }

  signInWithFacebook(): Promise<string> {
    if (!this.hasFacebook || !this.facebookReady) {
      return Promise.reject(new Error('Facebook Login no está configurado'));
    }

    return new Promise((resolve, reject) => {
      FB.login(
        (response) => {
          if (response.authResponse?.accessToken) {
            resolve(response.authResponse.accessToken);
          } else {
            reject(new Error('Inicio con Facebook cancelado'));
          }
        },
        { scope: 'email,public_profile' }
      );
    });
  }

  private loadGoogle(): Promise<void> {
    return new Promise((resolve, reject) => {
      if ((window as unknown as { google?: unknown }).google) {
        this.googleReady = true;
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        this.googleReady = true;
        resolve();
      };
      script.onerror = () => reject(new Error('No se pudo cargar Google Sign-In'));
      document.head.appendChild(script);
    });
  }

  private loadFacebook(): Promise<void> {
    return new Promise((resolve, reject) => {
      if ((window as unknown as { FB?: unknown }).FB) {
        this.facebookReady = true;
        resolve();
        return;
      }

      (window as unknown as { fbAsyncInit?: () => void }).fbAsyncInit = () => {
        FB.init({
          appId: environment.facebookAppId,
          cookie: true,
          xfbml: false,
          version: 'v19.0',
        });
        this.facebookReady = true;
        resolve();
      };

      if (!document.getElementById('facebook-jssdk')) {
        const script = document.createElement('script');
        script.id = 'facebook-jssdk';
        script.src = 'https://connect.facebook.net/es_ES/sdk.js';
        script.async = true;
        script.defer = true;
        script.onerror = () => reject(new Error('No se pudo cargar Facebook SDK'));
        document.body.appendChild(script);
      }
    });
  }
}
