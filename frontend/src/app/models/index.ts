export interface AuthUser {
  token: string;
  id: number;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'STUDENT';
  avatarUrl: string;
}

export interface DashboardStats {
  totalStudents: number;
  enabledStudents: number;
  casesCreated: number;
  blockedStudents: number;
  approvalRate: number;
}

export interface CaseCard {
  id: number;
  title: string;
  description: string;
  category: string;
  level: string;
  imageUrl: string;
  competencies: string[];
  studentStatus: string;
  attemptsUsed: number;
  maxAttempts: number;
  resetPending: boolean;
  studentsEnrolled: number;
}

export interface StudentStats {
  availableCases: number;
  completedCases: number;
  inProgressCases: number;
  blockedCases: number;
  attemptsUsed: number;
  bestScore: number;
  averageScore: number;
}

export interface StudentDashboard {
  stats: StudentStats;
  cases: CaseCard[];
}

export interface AnswerOption {
  id: number;
  text: string;
  orderIndex: number;
}

export interface Question {
  id: number;
  text: string;
  orderIndex: number;
  sceneImageUrl: string;
  options: AnswerOption[];
}

export interface CaseDetail {
  id: number;
  title: string;
  description: string;
  category: string;
  level: string;
  imageUrl: string;
  contextQuote: string;
  estimatedMinutes: number;
  complexityStars: number;
  competencies: string[];
  studentStatus: string;
  attemptsUsed: number;
  maxAttempts: number;
  blocked: boolean;
  resetPending: boolean;
  activeAttemptId: number | null;
  currentQuestionIndex: number;
  totalQuestions: number;
  currentQuestion: Question | null;
}

export interface AttemptSummary {
  id: number;
  studentName: string;
  studentEmail: string;
  caseTitle: string;
  attemptNumber: number;
  date: string;
  totalScore: number;
  status: string;
  clinicalScore: number;
  ethicalScore: number;
  normativeScore: number;
}

export interface ReportsSummary {
  approvalRate: number;
  approvalChange: number;
  casesAttempted: number;
  avgEthical: number;
  avgClinical: number;
  avgNormative: number;
  attempts: AttemptSummary[];
}

export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'FACEBOOK';

export interface UserDto {
  id: number;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'STUDENT';
  avatarUrl: string;
  enabled: boolean;
  maxAttempts: number;
  blocked: boolean;
  totalAttempts: number;
  passedAttempts: number;
  averageScore: number;
  authProvider?: AuthProvider;
}

export interface UserDetail {
  user: UserDto;
  attempts: AttemptSummary[];
  averageClinical: number;
  averageEthical: number;
  averageNormative: number;
}

export type NotificationType =
  | 'INFO'
  | 'SUCCESS'
  | 'WARNING'
  | 'ERROR'
  | 'RESET_REQUEST'
  | 'RESET_APPROVED'
  | 'CASE_COMPLETED'
  | 'ACCOUNT';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  link: string | null;
  read: boolean;
  createdAt: string;
  timeAgo: string;
}

export interface LeaderboardEntry {
  userId: number;
  fullName: string;
  avatarUrl: string;
  bestScore: number;
  averageScore: number;
  attempts: number;
  passed: number;
}

export interface AdminStats {
  totalStudents: number;
  enabledStudents: number;
  disabledStudents: number;
  blockedStudents: number;
  totalAdmins: number;
  casesCreated: number;
  totalAttempts: number;
  passedAttempts: number;
  failedAttempts: number;
  inProgressAttempts: number;
  approvalRate: number;
  avgClinical: number;
  avgEthical: number;
  avgNormative: number;
  pendingResetRequests: number;
  leaderboard: LeaderboardEntry[];
}

export interface ResetRequestSummary {
  id: number;
  userId: number;
  studentName: string;
  studentEmail: string;
  studentAvatar: string;
  caseId: number;
  caseTitle: string;
  approved: boolean;
  requestedAt: string;
  timeAgo: string;
}

export interface AttemptItem {
  questionText: string;
  selectedAnswer: string;
  correct: boolean;
  category: string;
}

export interface AttemptDetail {
  summary: AttemptSummary;
  items: AttemptItem[];
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  role: 'ADMIN' | 'STUDENT';
  avatarUrl?: string;
  maxAttempts?: number;
}

export interface CaseAdmin {
  id: number;
  title: string;
  description: string;
  category: string;
  level: string;
  imageUrl: string;
  contextQuote: string;
  estimatedMinutes: number;
  complexityStars: number;
  competencies: string[];
  questionCount: number;
  attemptsCount: number;
}

export interface CaseFormRequest {
  title: string;
  description: string;
  category: string;
  level: string;
  imageUrl: string;
  contextQuote: string;
  estimatedMinutes: number;
  complexityStars: number;
  competencies: string[];
}

export interface GroupMember {
  id: number;
  fullName: string;
  email: string;
  avatarUrl: string;
}

export interface StudentGroup {
  id: number;
  name: string;
  description: string;
  memberCount: number;
  members: GroupMember[];
  createdAt: string;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  studentIds?: number[];
}
