export const ATTEMPT_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: 'Disponible',
  IN_PROGRESS: 'En curso',
  PASSED: 'Aprobado',
  FAILED: 'No aprobado',
  BLOCKED: 'Bloqueado (sin intentos)',
  IN_PROGRESS_ATTEMPT: 'En progreso',
};

export const ATTEMPT_STATUS_BADGE: Record<string, string> = {
  PASSED: 'Aprobado',
  FAILED: 'No aprobado',
  BLOCKED: 'Bloqueado',
  IN_PROGRESS: 'En curso',
};

export function studentCaseStatusLabel(status: string): string {
  return ATTEMPT_STATUS_LABELS[status] || status;
}

export function attemptStatusLabel(status: string): string {
  return ATTEMPT_STATUS_BADGE[status] || status;
}
