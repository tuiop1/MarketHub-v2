import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
@Component({
  selector: 'mh-pagination',
  styleUrl: './pagination.component.scss',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly displayPages = computed(() => Math.max(this.totalPages(), 1));
}
