import {
  Component,
  ElementRef,
  HostListener,
  Input,
  forwardRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface GlassSelectOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-glass-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './glass-select.component.html',
  styleUrl: './glass-select.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GlassSelectComponent),
      multi: true,
    },
  ],
})
export class GlassSelectComponent implements ControlValueAccessor {
  @Input() options: GlassSelectOption[] = [];
  @Input() placeholder = 'Seleccionar';
  @Input() disabled = false;
  @Input() compact = false;

  private el = inject(ElementRef<HTMLElement>);

  value = '';
  isOpen = false;

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  get selectedLabel(): string {
    return this.options.find((o) => o.value === this.value)?.label ?? this.placeholder;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target as Node)) {
      this.isOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.isOpen = false;
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }

  toggle(): void {
    if (this.disabled) return;
    this.isOpen = !this.isOpen;
    if (this.isOpen) this.onTouched();
  }

  select(option: GlassSelectOption): void {
    this.value = option.value;
    this.onChange(option.value);
    this.isOpen = false;
    this.onTouched();
  }
}
