export const CASE_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: 'Disponible',
  IN_PROGRESS: 'En curso',
  PASSED: 'Aprobado',
  FAILED: 'No aprobado',
  BLOCKED: 'Bloqueado (sin intentos)',
};

export const ATTEMPT_STATUS_LABELS: Record<string, string> = {
  PASSED: 'Aprobado',
  FAILED: 'No aprobado',
  BLOCKED: 'Bloqueado',
  IN_PROGRESS: 'En curso',
};

export const SCORE_CATEGORY_LABELS: Record<string, string> = {
  CLINICAL: 'Clínica',
  ETHICAL: 'Ética',
  NORMATIVE: 'Normativa',
};

export function studentCaseStatusLabel(status: string): string {
  return CASE_STATUS_LABELS[status] || status;
}

export function attemptStatusLabel(status: string): string {
  return ATTEMPT_STATUS_LABELS[status] || status;
}

export function scoreCategoryLabel(category: string): string {
  return SCORE_CATEGORY_LABELS[category?.toUpperCase()] || category;
}
