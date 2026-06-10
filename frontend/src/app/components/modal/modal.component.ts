import { Component, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalService } from '../../services/modal.service';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  modal = inject(ModalService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modal.modal()) this.modal.cancel();
  }

  iconFor(variant: string): string {
    const map: Record<string, string> = {
      danger: 'delete_forever',
      warning: 'warning',
      success: 'check_circle',
      error: 'error',
      info: 'info',
      default: 'help',
    };
    return map[variant] ?? 'help';
  }

  isSingleAction(type: string): boolean {
    return type !== 'confirm' && type !== 'prompt';
  }
}
