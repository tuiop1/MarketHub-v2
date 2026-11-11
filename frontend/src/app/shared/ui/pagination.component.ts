import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
@Component({
  selector: 'mh-pagination',
  styleUrl: './pagination.component.css',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (totalPages() > 1 || page() > 0) {
    <nav class="pagination" aria-label="Pagination">
      @if (page() > 0) {
        <a
          class="button subtle"
          [routerLink]="[]"
          [queryParams]="{ page: page() - 1 }"
          queryParamsHandling="merge"
          aria-label="Previous page"
          >Previous</a
        >
      }
      <span aria-live="polite">Page {{ page() + 1 }} of {{ displayPages() }}</span>
      @if (page() + 1 < totalPages()) {
        <a
          class="button subtle"
          [routerLink]="[]"
          [queryParams]="{ page: page() + 1 }"
          queryParamsHandling="merge"
          aria-label="Next page"
          >Next</a
        >
      }
      @if (page() >= totalPages() && page() > 0) {
        <a
          class="button subtle"
          [routerLink]="[]"
          [queryParams]="{ page: 0 }"
          queryParamsHandling="merge"
          >First page</a
        >
      }
    </nav>
  }`,
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly displayPages = computed(() => Math.max(this.totalPages(), 1));
}
