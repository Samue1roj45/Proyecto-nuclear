import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { ConfirmDialogService } from '../../services/confirm-dialog.service';
import { ModalService } from '../../services/modal.service';
import { ToastService } from '../../services/toast.service';
import { NotificationStore } from '../../services/notification-store.service';
import { AccessRequestSummary } from '../../models';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';

@Component({
  selector: 'app-access-requests',
  standalone: true,
  imports: [CommonModule, UserAvatarComponent],
  templateUrl: './access-requests.component.html',
})
export class AccessRequestsComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private modal = inject(ModalService);
  private confirmDialog = inject(ConfirmDialogService);
  private store = inject(NotificationStore);

  requests: AccessRequestSummary[] = [];
  loading = true;
  pendingOnly = true;

  ngOnInit(): void {
    this.load();
  }

  async resetAll(): Promise<void> {
    const confirmed = await this.confirmDialog.confirm({
      title: 'Reiniciar todos los accesos',
      message: '¿Eliminar todas las solicitudes y códigos de acceso de estudiantes? Todos deberán solicitar acceso de nuevo.',
      confirmLabel: 'Reiniciar todos',
      variant: 'danger',
    });
    if (!confirmed) return;
    this.api.resetAllAccess().subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al reiniciar accesos'),
    });
  }

  load(): void {
    this.loading = true;
    this.api.getAccessRequests(this.pendingOnly).subscribe({
      next: (list) => {
        this.requests = list;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  togglePending(): void {
    this.pendingOnly = !this.pendingOnly;
    this.load();
  }

  async approve(r: AccessRequestSummary): Promise<void> {
    const confirmed = await this.modal.confirm({
      title: 'Aprobar acceso',
      message:
        `¿Aprobar el ingreso de ${r.studentName}?\n\n` +
        'Se generará un código único de 6 dígitos válido por 10 minutos. ' +
        'Si hay correo configurado, se enviará automáticamente; si no, deberás dárselo al estudiante.',
      confirmLabel: 'Aprobar y generar código',
      variant: 'default',
    });
    if (!confirmed) return;

    this.api.approveAccessRequest(r.id).subscribe({
      next: async (res) => {
        if (res.emailSent) {
          await this.modal.success(
            `${r.studentName} recibirá el código en su correo.\n\nVálido por ${res.expiresInMinutes} minutos.\nEl estudiante debe ingresarlo en la pantalla de login.`,
            'Código enviado por correo'
          );
        } else {
          await this.modal.info(
            `Estudiante: ${r.studentName}\nCódigo: ${res.code}\nVálido por: ${res.expiresInMinutes} minutos\nExpira: ${res.expiresAt}\n\nComunícale este código de forma segura. El estudiante ya puede ingresarlo en login.`,
            'Código generado'
          );
        }
        this.toast.success(res.message);
        this.load();
        this.store.refreshCount();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al aprobar'),
    });
  }

  async reject(r: AccessRequestSummary): Promise<void> {
    const confirmed = await this.modal.confirm({
      title: 'Rechazar acceso',
      message: `¿Rechazar la solicitud de ${r.studentName}?`,
      confirmLabel: 'Rechazar',
      variant: 'danger',
    });
    if (!confirmed) return;

    this.api.rejectAccessRequest(r.id).subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.load();
      },
      error: (err) => this.toast.error(err.error?.message || 'Error al rechazar'),
    });
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      APROBADO: 'Aprobado',
      RECHAZADO: 'Rechazado',
      EXPIRADO: 'Expirado',
      UTILIZADO: 'Utilizado',
    };
    return map[status] ?? status;
  }
}
