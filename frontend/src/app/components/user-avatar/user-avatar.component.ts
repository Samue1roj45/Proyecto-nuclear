import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { displayAvatarUrl, initialsAvatarUrl } from '../../utils/avatar';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `<img [src]="src" [class]="imgClass" [alt]="alt" (error)="onError($event)" />`,
})
export class UserAvatarComponent {
  @Input() avatarUrl?: string | null;
  @Input() fullName = '';
  @Input() imgClass = 'w-10 h-10 rounded-full object-cover';
  @Input() alt = '';

  get src(): string {
    return displayAvatarUrl(this.avatarUrl, this.fullName);
  }

  onError(event: Event): void {
    (event.target as HTMLImageElement).src = initialsAvatarUrl(this.fullName);
  }
}
