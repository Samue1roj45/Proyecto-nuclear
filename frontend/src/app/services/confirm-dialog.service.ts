import { Injectable } from '@angular/core';
import { ConfirmOptions, ModalService } from './modal.service';

/** @deprecated Usa ModalService directamente */
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  constructor(private modal: ModalService) {}

  confirm(options: string | ConfirmOptions) {
    return this.modal.confirm(options);
  }
}
