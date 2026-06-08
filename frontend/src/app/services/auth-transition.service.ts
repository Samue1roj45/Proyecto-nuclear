import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthTransitionService {
  entering = signal(false);

  startEnter(): void {
    this.entering.set(true);
    setTimeout(() => this.entering.set(false), 1400);
  }
}
