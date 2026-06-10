import { Injectable } from '@angular/core';
import { AccessFlowState, LoginStepResponse } from '../models/access-flow';

const STORAGE_KEY = 'psicosocial_access_flow';

@Injectable({ providedIn: 'root' })
export class AccessFlowService {
  saveFromLogin(res: LoginStepResponse): void {
    if (!res.accessSession || res.step === 'DIRECT_ACCESS') {
      this.clear();
      return;
    }

    const step = res.step === 'CODE_REQUIRED' ? 'access-code' : 'access-pending';
    this.save({
      email: res.email,
      studentName: res.studentName || '',
      accessSession: res.accessSession,
      step,
      expiresInMinutes: res.expiresInMinutes,
      expiresAt: res.expiresAt,
      emailSent: res.emailSent,
    });
  }

  save(state: AccessFlowState): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  load(): AccessFlowState | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AccessFlowState;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEY);
  }
}
