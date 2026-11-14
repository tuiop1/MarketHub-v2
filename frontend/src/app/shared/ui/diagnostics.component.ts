import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
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
  private readonly document = inject(DOCUMENT);

  close(): void {
    this.diagnostics.setEnabled(false);
    const target =
      this.document.getElementById('admin-diagnostics-switch') ??
      this.document.getElementById('main-content');
    target?.focus();
  }
}
