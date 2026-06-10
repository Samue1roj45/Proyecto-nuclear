import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ConfirmDialogService } from '../../services/confirm-dialog.service';
import { ToastService } from '../../services/toast.service';
import { CaseAdmin, CreateGroupRequest, StudentGroup, UserDto } from '../../models';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule, FormsModule, UserAvatarComponent],
  templateUrl: './groups.component.html',
})
export class GroupsComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private confirmDialog = inject(ConfirmDialogService);
  private route = inject(ActivatedRoute);

  groups: StudentGroup[] = [];
  students: UserDto[] = [];
  cases: CaseAdmin[] = [];
  loading = true;
  saving = false;
  showModal = false;
  editingId: number | null = null;
  search = '';

  form: CreateGroupRequest = this.emptyForm();
  selectedStudentIds = new Set<number>();
  selectedCaseIds = new Set<number>();

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.search = params['search'] || '';
    });
    this.load();
    this.loadStudents();
    this.loadCases();
  }

  loadCases(): void {
    this.api.getAdminCases().subscribe({
      next: (list) => (this.cases = list || []),
    });
  }

  emptyForm(): CreateGroupRequest {
    return { name: '', description: '', studentIds: [] };
  }

  load(): void {
    this.loading = true;
    this.api.getGroups().subscribe({
      next: (list) => {
        this.groups = (list || []).map((g) => ({
          ...g,
          members: g.members || [],
        }));
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err.error?.message || 'No se pudieron cargar los grupos');
      },
    });
  }

  loadStudents(): void {
    this.api.getUsers('', 'STUDENT', 'ALL').subscribe({
      next: (list) => (this.students = list || []),
      error: () => this.toast.error('No se pudieron cargar los estudiantes'),
    });
  }

  get filteredGroups(): StudentGroup[] {
    const q = this.search.trim().toLowerCase();
    if (!q) return this.groups;
    return this.groups.filter((g) => {
      const members = g.members || [];
      return (
        g.name.toLowerCase().includes(q) ||
        (g.description || '').toLowerCase().includes(q) ||
        members.some(
          (m) => m.fullName.toLowerCase().includes(q) || m.email.toLowerCase().includes(q)
        )
      );
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.selectedStudentIds = new Set();
    this.selectedCaseIds = new Set();
    this.showModal = true;
  }

  openEdit(group: StudentGroup): void {
    const members = group.members || [];
    this.editingId = group.id;
    this.form = {
      name: group.name,
      description: group.description,
      studentIds: members.map((m) => m.id),
      caseIds: group.assignedCaseIds || [],
    };
    this.selectedStudentIds = new Set(members.map((m) => m.id));
    this.selectedCaseIds = new Set(group.assignedCaseIds || []);
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.saving = false;
  }

  toggleStudent(id: number, checked: boolean): void {
    const next = new Set(this.selectedStudentIds);
    if (checked) {
      next.add(id);
    } else {
      next.delete(id);
    }
    this.selectedStudentIds = next;
  }

  isSelected(id: number): boolean {
    return this.selectedStudentIds.has(id);
  }

  toggleCase(id: number, checked: boolean): void {
    const next = new Set(this.selectedCaseIds);
    if (checked) next.add(id);
    else next.delete(id);
    this.selectedCaseIds = next;
  }

  isCaseSelected(id: number): boolean {
    return this.selectedCaseIds.has(id);
  }

  save(): void {
    if (!this.form.name.trim()) {
      this.toast.error('El nombre del grupo es obligatorio');
      return;
    }

    this.saving = true;
    const payload: CreateGroupRequest = {
      name: this.form.name.trim(),
      description: this.form.description?.trim() || '',
      studentIds: Array.from(this.selectedStudentIds),
      caseIds: Array.from(this.selectedCaseIds),
    };

    const req = this.editingId
      ? this.api.updateGroup(this.editingId, payload)
      : this.api.createGroup(payload);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.toast.success(this.editingId ? 'Grupo actualizado' : 'Grupo creado');
        this.closeModal();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.toast.error(err.error?.message || 'Error al guardar el grupo');
      },
    });
  }

  async remove(group: StudentGroup): Promise<void> {
    const confirmed = await this.confirmDialog.confirm({
      title: 'Eliminar grupo',
      message: `¿Eliminar el grupo "${group.name}"?`,
      confirmLabel: 'Eliminar',
      variant: 'danger',
    });
    if (!confirmed) return;
    this.api.deleteGroup(group.id).subscribe({
      next: () => {
        this.toast.success('Grupo eliminado');
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al eliminar'),
    });
  }
}
