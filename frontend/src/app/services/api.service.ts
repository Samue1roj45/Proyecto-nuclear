import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import {
  AdminStats,
  AppNotification,
  AttemptDetail,
  AttemptSummary,
  CaseAdmin,
  CaseDetail,
  CaseFormRequest,
  HelpSection,
  QuestionAdmin,
  QuestionFormRequest,
  SubmitAnswerResponse,
  CreateGroupRequest,
  CreateUserRequest,
  LeaderboardEntry,
  StudentGroup,
  ReportsSummary,
  AccessRequestSummary,
  ApproveAccessResponse,
  ResetRequestSummary,
  StudentDashboard,
  UserDetail,
  UserDto,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  private headers() {
    return new HttpHeaders({
      Authorization: `Bearer ${this.auth.token}`,
    });
  }

  getDashboard(search?: string) {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    return this.http.get<StudentDashboard>(`${environment.apiUrl}/dashboard`, {
      headers: this.headers(),
      params,
    });
  }

  getCase(id: number) {
    return this.http.get<CaseDetail>(`${environment.apiUrl}/cases/${id}`, {
      headers: this.headers(),
    });
  }

  startCase(id: number) {
    return this.http.post<CaseDetail>(`${environment.apiUrl}/cases/${id}/start`, {}, {
      headers: this.headers(),
    });
  }

  submitAnswer(caseId: number, questionId: number, optionId: number, elapsedSeconds?: number) {
    return this.http.post<SubmitAnswerResponse>(
      `${environment.apiUrl}/cases/${caseId}/answer`,
      { questionId, optionId, elapsedSeconds },
      { headers: this.headers() }
    );
  }

  requestReset(caseId: number) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/cases/${caseId}/reset-request`,
      {},
      { headers: this.headers() }
    );
  }

  getReports(search?: string, status?: string, sort?: string, page = 0, pageSize = 20) {
    let params = new HttpParams().set('page', page).set('pageSize', pageSize);
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    if (sort) params = params.set('sort', sort);
    return this.http.get<ReportsSummary>(`${environment.apiUrl}/reports`, {
      headers: this.headers(),
      params,
    });
  }

  getHelp() {
    return this.http.get<HelpSection[]>(`${environment.apiUrl}/help`);
  }

  getAttemptDetail(id: number) {
    return this.http.get<AttemptSummary>(`${environment.apiUrl}/reports/attempts/${id}`, {
      headers: this.headers(),
    });
  }

  getAttemptFullDetail(id: number) {
    return this.http.get<AttemptDetail>(`${environment.apiUrl}/reports/attempts/${id}/full`, {
      headers: this.headers(),
    });
  }

  exportReportsCsv(search?: string, status?: string) {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    return this.http.get(`${environment.apiUrl}/reports/export`, {
      headers: this.headers(),
      params,
      responseType: 'text',
    });
  }

  // ===== Users (admin) =====
  getUsers(search?: string, role?: string, status?: string) {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    if (role) params = params.set('role', role);
    if (status) params = params.set('status', status);
    return this.http.get<UserDto[]>(`${environment.apiUrl}/admin/users`, {
      headers: this.headers(),
      params,
    });
  }

  getUserDetail(id: number) {
    return this.http.get<UserDetail>(`${environment.apiUrl}/admin/users/${id}`, {
      headers: this.headers(),
    });
  }

  createUser(req: CreateUserRequest) {
    return this.http.post<UserDto>(`${environment.apiUrl}/admin/users`, req, {
      headers: this.headers(),
    });
  }

  updateUser(id: number, req: Partial<CreateUserRequest> & { enabled?: boolean }) {
    return this.http.put<UserDto>(`${environment.apiUrl}/admin/users/${id}`, req, {
      headers: this.headers(),
    });
  }

  deleteUser(id: number) {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/admin/users/${id}`, {
      headers: this.headers(),
    });
  }

  setUserEnabled(id: number, enabled: boolean) {
    return this.http.patch<UserDto>(
      `${environment.apiUrl}/admin/users/${id}/${enabled ? 'enable' : 'disable'}`,
      {},
      { headers: this.headers() }
    );
  }

  changeUserRole(id: number, role: string) {
    return this.http.patch<UserDto>(
      `${environment.apiUrl}/admin/users/${id}/role`,
      { role },
      { headers: this.headers() }
    );
  }

  setUserMaxAttempts(id: number, maxAttempts: number) {
    return this.http.patch<UserDto>(
      `${environment.apiUrl}/admin/users/${id}/max-attempts`,
      { maxAttempts },
      { headers: this.headers() }
    );
  }

  resetUserAttempts(id: number) {
    return this.http.post<UserDto>(
      `${environment.apiUrl}/admin/users/${id}/reset-attempts`,
      {},
      { headers: this.headers() }
    );
  }

  // ===== Notifications =====
  getNotifications(unreadOnly = false) {
    let params = new HttpParams();
    if (unreadOnly) params = params.set('unreadOnly', 'true');
    return this.http.get<AppNotification[]>(`${environment.apiUrl}/notifications`, {
      headers: this.headers(),
      params,
    });
  }

  getUnreadCount() {
    return this.http.get<{ count: number }>(`${environment.apiUrl}/notifications/unread-count`, {
      headers: this.headers(),
    });
  }

  markNotificationRead(id: number) {
    return this.http.patch<{ message: string }>(
      `${environment.apiUrl}/notifications/${id}/read`,
      {},
      { headers: this.headers() }
    );
  }

  markAllNotificationsRead() {
    return this.http.patch<{ message: string }>(
      `${environment.apiUrl}/notifications/read-all`,
      {},
      { headers: this.headers() }
    );
  }

  deleteNotification(id: number) {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/notifications/${id}`, {
      headers: this.headers(),
    });
  }

  clearNotifications() {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/notifications`, {
      headers: this.headers(),
    });
  }

  // ===== Profile =====
  getProfile() {
    return this.http.get<UserDto>(`${environment.apiUrl}/profile`, {
      headers: this.headers(),
    });
  }

  updateProfile(req: { fullName?: string; avatarUrl?: string }) {
    return this.http.put<UserDto>(`${environment.apiUrl}/profile`, req, {
      headers: this.headers(),
    });
  }

  uploadAvatar(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UserDto>(`${environment.apiUrl}/profile/avatar`, formData, {
      headers: this.headers(),
    });
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/profile/change-password`,
      { currentPassword, newPassword },
      { headers: this.headers() }
    );
  }

  // ===== Admin dashboard / reset requests =====
  getAdminStats() {
    return this.http.get<AdminStats>(`${environment.apiUrl}/admin/stats`, {
      headers: this.headers(),
    });
  }

  getLeaderboard() {
    return this.http.get<LeaderboardEntry[]>(`${environment.apiUrl}/admin/leaderboard`, {
      headers: this.headers(),
    });
  }

  getResetRequests(pendingOnly = true) {
    let params = new HttpParams().set('pendingOnly', String(pendingOnly));
    return this.http.get<ResetRequestSummary[]>(`${environment.apiUrl}/admin/reset-requests`, {
      headers: this.headers(),
      params,
    });
  }

  approveReset(id: number) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/admin/reset-requests/${id}/approve`,
      {},
      { headers: this.headers() }
    );
  }

  rejectReset(id: number) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/admin/reset-requests/${id}/reject`,
      {},
      { headers: this.headers() }
    );
  }

  getAccessRequests(pendingOnly = true) {
    let params = new HttpParams().set('pendingOnly', String(pendingOnly));
    return this.http.get<AccessRequestSummary[]>(`${environment.apiUrl}/admin/access-requests`, {
      headers: this.headers(),
      params,
    });
  }

  approveAccessRequest(id: number) {
    return this.http.post<ApproveAccessResponse>(
      `${environment.apiUrl}/admin/access-requests/${id}/approve`,
      {},
      { headers: this.headers() }
    );
  }

  rejectAccessRequest(id: number) {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/admin/access-requests/${id}/reject`,
      {},
      { headers: this.headers() }
    );
  }

  // ===== Cases (admin) =====
  getAdminCases(search?: string) {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    return this.http.get<CaseAdmin[]>(`${environment.apiUrl}/admin/cases`, {
      headers: this.headers(),
      params,
    });
  }

  createCase(req: CaseFormRequest) {
    return this.http.post<CaseAdmin>(`${environment.apiUrl}/admin/cases`, req, {
      headers: this.headers(),
    });
  }

  updateCase(id: number, req: CaseFormRequest) {
    return this.http.put<CaseAdmin>(`${environment.apiUrl}/admin/cases/${id}`, req, {
      headers: this.headers(),
    });
  }

  deleteCase(id: number) {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/admin/cases/${id}`, {
      headers: this.headers(),
    });
  }

  getCaseQuestions(caseId: number) {
    return this.http.get<QuestionAdmin[]>(`${environment.apiUrl}/admin/cases/${caseId}/questions`, {
      headers: this.headers(),
    });
  }

  addQuestion(caseId: number, req: QuestionFormRequest) {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/admin/cases/${caseId}/questions`, req, {
      headers: this.headers(),
    });
  }

  updateQuestion(questionId: number, req: QuestionFormRequest) {
    return this.http.put<QuestionAdmin>(`${environment.apiUrl}/admin/questions/${questionId}`, req, {
      headers: this.headers(),
    });
  }

  reorderQuestions(caseId: number, questionIds: number[]) {
    return this.http.put<{ message: string }>(
      `${environment.apiUrl}/admin/cases/${caseId}/questions/reorder`,
      { questionIds },
      { headers: this.headers() },
    );
  }

  deleteQuestion(questionId: number) {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/admin/questions/${questionId}`, {
      headers: this.headers(),
    });
  }

  resetAllAccess() {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/admin/access-requests/reset-all`, {}, {
      headers: this.headers(),
    });
  }

  // ===== Groups (admin) =====
  getGroups() {
    return this.http.get<StudentGroup[]>(`${environment.apiUrl}/admin/groups`, {
      headers: this.headers(),
    });
  }

  createGroup(req: CreateGroupRequest) {
    return this.http.post<StudentGroup>(`${environment.apiUrl}/admin/groups`, req, {
      headers: this.headers(),
    });
  }

  updateGroup(id: number, req: Partial<CreateGroupRequest>) {
    return this.http.put<StudentGroup>(`${environment.apiUrl}/admin/groups/${id}`, req, {
      headers: this.headers(),
    });
  }

  deleteGroup(id: number) {
    return this.http.delete<{ message: string }>(`${environment.apiUrl}/admin/groups/${id}`, {
      headers: this.headers(),
    });
  }
}
