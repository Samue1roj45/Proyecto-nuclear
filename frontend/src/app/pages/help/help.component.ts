import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { HelpSection } from '../../models';

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help.component.html',
})
export class HelpComponent implements OnInit {
  private api = inject(ApiService);
  sections: HelpSection[] = [];
  loading = true;
  expanded: Record<string, boolean> = {};

  ngOnInit(): void {
    this.api.getHelp().subscribe({
      next: (data) => {
        this.sections = data;
        if (data.length) this.expanded[data[0].id] = true;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  toggle(id: string): void {
    this.expanded[id] = !this.expanded[id];
  }
}
