import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { pagedResource } from '../../../shared/state/paged-resource';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../../shared/ui/feedback.component';
import { PaginationComponent } from '../../../shared/ui/pagination.component';
import { IconComponent } from '../../../shared/ui/icon.component';
import { CheckoutComponent } from '../../orders/components/checkout.component';
import { PurchaseItem } from '../../orders/data-access/orders-api.service';
import { CatalogApi } from '../data-access/catalog-api.service';
import { ProductCardComponent } from '../components/product-card.component';
const sorts = ['name,asc', 'priceCents,asc', 'priceCents,desc', 'createdAt,desc'];
@Component({
  imports: [
    RouterLink,
    ProductCardComponent,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
    IconComponent,
    CheckoutComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-list.page.html',
  styleUrl: './product-list.page.scss',
})
export class ProductListPage {
  readonly auth = inject(AuthService);
  private readonly api = inject(CatalogApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly resource = pagedResource((page, sort) => this.api.products(page, sort), sorts);
  readonly sort = computed(() => {
    this.resource.state();
    const value = this.route.snapshot.queryParamMap.get('sort');
    return value && sorts.includes(value) ? value : sorts[0];
  });
  readonly selection = signal<Readonly<Record<string, PurchaseItem>>>({});
  readonly checkoutBusy = signal(false);
  readonly selectedItems = computed(
    () =>
      this.resource
        .state()
        .value?.content.flatMap((p) => (this.selection()[p.id] ? [this.selection()[p.id]] : [])) ??
      []
  );
  readonly selectedTotal = computed(
    () =>
      this.resource
        .state()
        .value?.content.reduce(
          (sum, p) => sum + p.priceCents * (this.selection()[p.id]?.quantity ?? 0),
          0
        ) ?? 0
  );
  select(id: string, item: PurchaseItem | null): void {
    this.selection.update((current) => {
      const next = { ...current };
      if (item) next[id] = item;
      else delete next[id];
      return next;
    });
  }
  changeSort(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement) || this.checkoutBusy()) return;
    this.selection.set({});
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sort: event.target.value, page: 0 },
      queryParamsHandling: 'merge',
    });
  }
}
