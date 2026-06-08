import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { CreateUserRequest, UserDto } from '../../models';
import { GlassSelectComponent } from '../../components/glass-select/glass-select.component';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, GlassSelectComponent, UserAvatarComponent],
  templateUrl: './users.component.html',
})
export class UsersComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  users: UserDto[] = [];
  loading = true;
  search = '';
  roleFilter = 'ALL';
  statusFilter = 'ALL';

  readonly roleFilterOptions = [
    { value: 'ALL', label: 'Todos los roles' },
    { value: 'STUDENT', label: 'Estudiantes' },
    { value: 'ADMIN', label: 'Profesores' },
  ];

  readonly statusFilterOptions = [
    { value: 'ALL', label: 'Todos los estados' },
    { value: 'ENABLED', label: 'Habilitados' },
    { value: 'DISABLED', label: 'Deshabilitados' },
    { value: 'BLOCKED', label: 'Bloqueados' },
  ];

  readonly roleFormOptions = [
    { value: 'STUDENT', label: 'Estudiante' },
    { value: 'ADMIN', label: 'Profesor' },
  ];

  showModal = false;
  editingId: number | null = null;
  form: CreateUserRequest = this.emptyForm();

  ngOnInit(): void {
    this.route.queryParams.subscribe((p) => {
      this.search = p['search'] || '';
      this.load();
    });
  }

  emptyForm(): CreateUserRequest {
    return { fullName: '', email: '', password: '', role: 'STUDENT', maxAttempts: 3 };
  }

  load(): void {
    this.loading = true;
    this.api.getUsers(this.search, this.roleFilter, this.statusFilter).subscribe({
      next: (list) => {
        this.users = list;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  get studentCount(): number {
    return this.users.filter((u) => u.role === 'STUDENT').length;
  }

  get blockedCount(): number {
    return this.users.filter((u) => u.blocked).length;
  }

  get disabledCount(): number {
    return this.users.filter((u) => !u.enabled).length;
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.showModal = true;
  }

  openEdit(u: UserDto): void {
    this.editingId = u.id;
    this.form = {
      fullName: u.fullName,
      email: u.email,
      password: '',
      role: u.role,
      maxAttempts: u.maxAttempts,
    };
    this.showModal = true;
  }

  save(): void {
    if (this.editingId) {
      this.api.updateUser(this.editingId, this.form).subscribe({
        next: () => {
          this.toast.success('Usuario actualizado');
          this.showModal = false;
          this.load();
        },
        error: (err) => this.toast.error(err.error?.message || 'Error al actualizar'),
      });
    } else {
      this.api.createUser(this.form).subscribe({
        next: () => {
          this.toast.success('Usuario creado');
          this.showModal = false;
          this.load();
        },
        error: (err) => this.toast.error(err.error?.message || 'Error al crear'),
      });
    }
  }

  toggleEnabled(u: UserDto): void {
    this.api.setUserEnabled(u.id, !u.enabled).subscribe({
      next: (updated) => {
        u.enabled = updated.enabled;
        this.toast.success(updated.enabled ? 'Usuario habilitado' : 'Usuario deshabilitado');
      },
    });
  }

  changeRole(u: UserDto): void {
    const newRole = u.role === 'ADMIN' ? 'STUDENT' : 'ADMIN';
    this.api.changeUserRole(u.id, newRole).subscribe({
      next: (updated) => {
        u.role = updated.role;
        this.toast.success('Rol actualizado a ' + updated.role);
      },
    });
  }

  resetAttempts(u: UserDto): void {
    const msg =
      `¿Reiniciar TODOS los intentos de ${u.fullName} en todos los casos?\n\n` +
      'También se aprobarán las solicitudes de reinicio pendientes de este estudiante.';
    if (!confirm(msg)) return;
    this.api.resetUserAttempts(u.id).subscribe({
      next: () => {
        this.toast.success('Todos los intentos reiniciados para ' + u.fullName);
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al reiniciar intentos'),
    });
  }

  updateMaxAttempts(u: UserDto, value: string): void {
    const n = parseInt(value, 10);
    if (isNaN(n) || n < 1) return;
    this.api.setUserMaxAttempts(u.id, n).subscribe({
      next: (updated) => {
        u.maxAttempts = updated.maxAttempts;
        this.toast.success('Intentos máximos actualizados');
      },
    });
  }

  remove(u: UserDto): void {
    if (!confirm(`¿Eliminar a ${u.fullName}? Esta acción no se puede deshacer.`)) return;
    this.api.deleteUser(u.id).subscribe({
      next: () => {
        this.toast.success('Usuario eliminado');
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al eliminar'),
    });
  }

  initials(name: string): string {
    return name.split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase();
  }
}
