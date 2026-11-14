import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DiagnosticsService } from '../../core/diagnostics/diagnostics.service';

@Component({
  selector: 'mh-diagnostics',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './diagnostics.component.html',
  styleUrl: './diagnostics.component.scss',
})
export class DiagnosticsComponent {
  readonly diagnostics = inject(DiagnosticsService);
  readonly collapsed = signal(false);

  toggle(event: Event): void {
    this.collapsed.set(false);
    this.diagnostics.setEnabled((event.target as HTMLInputElement).checked);
  }
}
