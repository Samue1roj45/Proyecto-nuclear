import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { UserDto } from '../../models';
import { displayAvatarUrl, hasCustomAvatar, initialsAvatarUrl } from '../../utils/avatar';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  profile: UserDto | null = null;
  fullName = '';
  avatarUrl = '';
  savingProfile = false;
  uploading = false;
  removingPhoto = false;

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  changingPassword = false;
  showNew = false;

  ngOnInit(): void {
    this.api.getProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.fullName = p.fullName;
        this.avatarUrl = p.avatarUrl || '';
      },
    });
  }

  get displayAvatar(): string {
    return displayAvatarUrl(this.avatarUrl || this.profile?.avatarUrl, this.fullName || this.profile?.fullName || '');
  }

  get hasPhoto(): boolean {
    return hasCustomAvatar(this.avatarUrl || this.profile?.avatarUrl);
  }

  get passwordStrength(): { label: string; value: number; color: string } {
    const v = this.newPassword.length;
    let score = 0;
    if (v >= 6) score++;
    if (v >= 10) score++;
    if (/[A-Z]/.test(this.newPassword) && /[0-9]/.test(this.newPassword)) score++;
    if (/[^A-Za-z0-9]/.test(this.newPassword)) score++;
    const map = [
      { label: 'Muy débil', value: 25, color: 'bg-error' },
      { label: 'Débil', value: 50, color: 'bg-error' },
      { label: 'Aceptable', value: 75, color: 'bg-secondary' },
      { label: 'Fuerte', value: 100, color: 'bg-primary' },
    ];
    return v === 0 ? { label: '', value: 0, color: 'bg-surface-container-high' } : map[Math.min(score, 4) - 1] || map[0];
  }

  triggerFile(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.toast.error('Selecciona un archivo de imagen válido');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toast.error('La imagen supera los 5MB');
      return;
    }

    this.uploading = true;
    const localPreview = URL.createObjectURL(file);
    this.avatarUrl = localPreview;

    this.api.uploadAvatar(file).subscribe({
      next: (p) => {
        this.profile = p;
        this.avatarUrl = p.avatarUrl || '';
        this.auth.updateStoredUser({ avatarUrl: p.avatarUrl });
        this.toast.success('Foto de perfil actualizada');
        this.uploading = false;
        URL.revokeObjectURL(localPreview);
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Error al subir la imagen');
        this.uploading = false;
        this.avatarUrl = this.profile?.avatarUrl || '';
      },
    });
    input.value = '';
  }

  removePhoto(): void {
    this.removingPhoto = true;
    this.api.updateProfile({ avatarUrl: '' }).subscribe({
      next: (p) => {
        this.profile = p;
        this.avatarUrl = '';
        this.auth.updateStoredUser({ avatarUrl: '' });
        this.toast.success('Foto de perfil eliminada');
        this.removingPhoto = false;
      },
      error: () => {
        this.toast.error('Error al quitar la foto');
        this.removingPhoto = false;
      },
    });
  }

  saveProfile(): void {
    this.savingProfile = true;
    this.api.updateProfile({
      fullName: this.fullName,
      avatarUrl: this.avatarUrl.trim(),
    }).subscribe({
      next: (p) => {
        this.profile = p;
        this.avatarUrl = p.avatarUrl || '';
        this.auth.updateStoredUser({ fullName: p.fullName, avatarUrl: p.avatarUrl });
        this.toast.success('Perfil actualizado');
        this.savingProfile = false;
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Error al actualizar');
        this.savingProfile = false;
      },
    });
  }

  changePassword(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.toast.error('Las contraseñas no coinciden');
      return;
    }
    if (this.newPassword.length < 6) {
      this.toast.error('La nueva contraseña debe tener al menos 6 caracteres');
      return;
    }
    this.changingPassword = true;
    this.api.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.currentPassword = this.newPassword = this.confirmPassword = '';
        this.changingPassword = false;
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Error al cambiar contraseña');
        this.changingPassword = false;
      },
    });
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).src = initialsAvatarUrl(this.fullName || this.profile?.fullName || '');
  }
}
