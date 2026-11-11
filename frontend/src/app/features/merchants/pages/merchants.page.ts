import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MerchantsApi } from '../data-access/merchants-api.service';
import { pagedResource } from '../../../shared/state/paged-resource';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../../shared/ui/feedback.component';
import { PaginationComponent } from '../../../shared/ui/pagination.component';
import { IconComponent } from '../../../shared/ui/icon.component';
@Component({
  imports: [
    RouterLink,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
    IconComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="page-heading">
      <div>
        <h1>Meet the shops.</h1>
        <p>Independent sellers, all in one place.</p>
      </div>
      <a class="button primary" routerLink="/register" [queryParams]="{ type: 'merchant' }"
        >Open a shop</a
      >
    </div>
    <div class="toolbar">
      <h2>Shop directory</h2>
      <label class="sort-label"
        >Sort by<select
          [value]="route.snapshot.queryParamMap.get('sort') || 'shopName,asc'"
          (change)="sort($event)"
        >
          <option value="shopName,asc">Shop name, A–Z</option>
          <option value="createdAt,desc">Newest first</option>
        </select></label
      >
    </div>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading shops…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as page) {
      <div class="directory-grid">
        @for (shop of page.content; track shop.id) {
          <a class="directory-card" [routerLink]="['/merchants', shop.id]"
            ><span class="shop-avatar">{{ shop.shopName.slice(0, 1) }}</span
            ><span class="badge good"><mh-icon name="check" /> Approved shop</span>
            <h2>{{ shop.shopName }}</h2>
            <p>{{ shop.description || 'An independent shop on MarketHub.' }}</p>
            <span class="link-label">Visit shop</span></a
          >
        }
      </div>
      @if (!page.content.length) {
        <mh-empty
          title="The next small shop could be yours."
          description="Approved shops will appear here. Apply to open your own shop on MarketHub."
          ><a class="button primary" routerLink="/register" [queryParams]="{ type: 'merchant' }"
            >Open a shop</a
          ></mh-empty
        >
      }
      <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
    }`,
})
export class MerchantsPage {
  private readonly api = inject(MerchantsApi);
  private readonly router = inject(Router);
  readonly route = inject(ActivatedRoute);
  readonly resource = pagedResource(
    (page, sort) => this.api.list(page, sort),
    ['shopName,asc', 'createdAt,desc']
  );
  sort(event: Event): void {
    if (event.target instanceof HTMLSelectElement)
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { page: 0, sort: event.target.value },
        queryParamsHandling: 'merge',
      });
  }
}
