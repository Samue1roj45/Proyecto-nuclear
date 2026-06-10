export interface AuthUser {
  token: string;
  id: number;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'STUDENT';
  avatarUrl: string;
}

export type LoginStep = 'DIRECT_ACCESS' | 'REQUEST_SUBMITTED' | 'AWAITING_APPROVAL' | 'CODE_REQUIRED';

export interface LoginStepResponse {
  step: LoginStep;
  message: string;
  detail?: string;
  email: string;
  studentName?: string;
  accessSession?: string;
  expiresInMinutes?: number;
  expiresAt?: string;
  emailSent?: boolean;
  auth?: AuthUser;
}

export type AccessCodeStatus = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'EXPIRADO' | 'UTILIZADO';

export interface AccessStatusResponse {
  status: AccessCodeStatus | null;
  message: string;
  detail?: string;
  canEnterCode: boolean;
  canRetry?: boolean;
  expiresInMinutes?: number;
  expiresAt?: string;
  studentName?: string;
}

export interface AccessFlowState {
  email: string;
  studentName: string;
  accessSession: string;
  step: 'access-pending' | 'access-code';
  expiresInMinutes?: number;
  expiresAt?: string;
  emailSent?: boolean;
}
