import { Injectable, signal } from '@angular/core';

export type ModalType = 'info' | 'success' | 'error' | 'warning' | 'confirm' | 'alert' | 'prompt';
export type ModalVariant = 'danger' | 'warning' | 'default' | 'success' | 'error' | 'info';

export interface ModalState {
  type: ModalType;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  inputValue: string;
  inputPlaceholder: string;
  variant: ModalVariant;
}

export interface ConfirmOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ModalVariant;
}

export interface PromptOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  placeholder?: string;
  defaultValue?: string;
}

@Injectable({ providedIn: 'root' })
export class ModalService {
  modal = signal<ModalState | null>(null);
  private resolveFn: ((value: boolean | string | null) => void) | null = null;

  alert(message: string, title = 'Aviso'): Promise<void> {
    return this.showSingle(message, title, 'alert', 'info', 'Entendido');
  }

  info(message: string, title = 'Información'): Promise<void> {
    return this.showSingle(message, title, 'info', 'info', 'Entendido');
  }

  success(message: string, title = 'Éxito'): Promise<void> {
    return this.showSingle(message, title, 'success', 'success', 'Entendido');
  }

  error(message: string, title = 'Error'): Promise<void> {
    return this.showSingle(message, title, 'error', 'error', 'Entendido');
  }

  warning(message: string, title = 'Advertencia'): Promise<void> {
    return this.showSingle(message, title, 'warning', 'warning', 'Entendido');
  }

  confirm(options: string | ConfirmOptions): Promise<boolean> {
    const opts = typeof options === 'string' ? { message: options } : options;
    return new Promise((resolve) => {
      this.resolveFn = (value) => resolve(!!value);
      this.modal.set({
        type: 'confirm',
        title: opts.title ?? 'Confirmar acción',
        message: opts.message,
        confirmLabel: opts.confirmLabel ?? 'Aceptar',
        cancelLabel: opts.cancelLabel ?? 'Cancelar',
        inputValue: '',
        inputPlaceholder: '',
        variant: opts.variant ?? 'default',
      });
    });
  }

  prompt(options: string | PromptOptions): Promise<string | null> {
    const opts = typeof options === 'string' ? { message: options } : options;
    return new Promise((resolve) => {
      this.resolveFn = (value) => resolve(typeof value === 'string' ? value : null);
      this.modal.set({
        type: 'prompt',
        title: opts.title ?? 'Ingresa un valor',
        message: opts.message,
        confirmLabel: opts.confirmLabel ?? 'Aceptar',
        cancelLabel: opts.cancelLabel ?? 'Cancelar',
        inputValue: opts.defaultValue ?? '',
        inputPlaceholder: opts.placeholder ?? '',
        variant: 'default',
      });
    });
  }

  accept(): void {
    const current = this.modal();
    if (!current) return;
    if (current.type === 'prompt') {
      this.resolveFn?.(current.inputValue.trim());
    } else {
      this.resolveFn?.(true);
    }
    this.cleanup();
  }

  cancel(): void {
    if (this.modal()?.type === 'prompt') {
      this.resolveFn?.(null);
    } else {
      this.resolveFn?.(false);
    }
    this.cleanup();
  }

  updateInput(value: string): void {
    const current = this.modal();
    if (!current) return;
    this.modal.set({ ...current, inputValue: value });
  }

  private showSingle(
    message: string,
    title: string,
    type: ModalType,
    variant: ModalVariant,
    confirmLabel: string,
  ): Promise<void> {
    return new Promise((resolve) => {
      this.resolveFn = () => resolve();
      this.modal.set({
        type,
        title,
        message,
        confirmLabel,
        cancelLabel: '',
        inputValue: '',
        inputPlaceholder: '',
        variant,
      });
    });
  }

  private cleanup(): void {
    this.modal.set(null);
    this.resolveFn = null;
  }
}
