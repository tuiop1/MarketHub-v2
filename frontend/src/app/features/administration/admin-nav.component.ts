import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { DiagnosticsService } from '../../core/diagnostics/diagnostics.service';
@Component({
  selector: 'mh-admin-nav',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-nav.component.html',
  styleUrl: './admin-nav.component.scss',
})
export class AdminNavComponent {
  readonly diagnostics = inject(DiagnosticsService);

  toggleDiagnostics(event: Event): void {
    this.diagnostics.setEnabled((event.target as HTMLInputElement).checked);
  }
}
