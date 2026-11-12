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
  template: ` <section class="market-intro">
      <div>
        <h1>A little discovery.<br />A good find.</h1>
        <p>
          Explore products from independent shops.<br />Find something that belongs in your
          everyday.
        </p>
        <a class="button primary" routerLink="/merchants"
          >Meet the shops <mh-icon name="arrow"
        /></a>
      </div>
      <aside class="intro-aside">
        <mh-icon name="shop" />
        <h2>Good things start<br />with a small shop.</h2>
        <a routerLink="/register" [queryParams]="{ type: 'merchant' }">Make room for yours</a>
      </aside>
    </section>
    <section aria-labelledby="products-heading">
      <div class="toolbar">
        <div>
          <h2 id="products-heading">The marketplace</h2>
          <p class="u-text-muted u-text-small">
            @if (resource.state().value; as page) {
              {{ page.totalElements }} products to explore
            } @else {
              Products from the MarketHub catalog
            }
          </p>
        </div>
        <label class="sort-label"
          >Sort by<select [value]="sort()" (change)="changeSort($event)">
            <option value="name,asc">Name, A–Z</option>
            <option value="priceCents,asc">Price, low to high</option>
            <option value="priceCents,desc">Price, high to low</option>
            <option value="createdAt,desc">Newest first</option>
          </select></label
        >
      </div>
      @if (resource.state().status === 'loading') {
        <mh-loading label="Finding products…" />
      }
      <mh-feedback
        [error]="resource.state().error"
        [retryable]="true"
        (retry)="resource.reload()"
      />
      @if (resource.state().value; as page) {
        @if (page.content.length) {
          <div class="product-grid">
            @for (product of page.content; track product.id) {
              <mh-product-card
                [product]="product"
                [customer]="auth.hasRole('CUSTOMER')"
                [selected]="!!selection()[product.id]"
                [locked]="checkoutBusy()"
                (selection)="select(product.id, $event)"
              />
            }
          </div>
        } @else {
          <mh-empty
            title="No products here yet."
            description="New listings will appear here when shops add products. You can explore the shop directory in the meantime."
            ><a class="button subtle" routerLink="/merchants">Browse shops</a></mh-empty
          >
        }
        <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
      }
      @if (selectedItems().length) {
        <div class="direct-checkout">
          <h2>{{ selectedItems().length }} selected for direct checkout</h2>
          <p class="u-text-muted">
            These products will be purchased directly. Your shopping cart will stay as it is.
          </p>
          <mh-checkout
            [items]="selectedItems()"
            [total]="selectedTotal()"
            (busyChange)="checkoutBusy.set($event)"
          />
        </div>
      }
    </section>`,
  styleUrl: './product-list.page.css',
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
