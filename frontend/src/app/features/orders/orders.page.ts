import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrdersApi } from './data-access/orders-api.service';
import { OrderDetailsComponent } from './components/order-details.component';
import { pagedResource } from '../../shared/state/paged-resource';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../shared/ui/feedback.component';
import { PaginationComponent } from '../../shared/ui/pagination.component';
@Component({
  imports: [
    RouterLink,
    OrderDetailsComponent,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-heading">
      <div>
        <h1>Your orders.</h1>
        <p>Every find, all in one place. Open an order to see its items and status.</p>
      </div>
      <button
        class="button subtle"
        [disabled]="resource.state().status === 'loading'"
        (click)="resource.reload()"
      >
        Refresh orders
      </button>
    </div>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading orders…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as page) {
      @for (order of page.content; track order.id) {
        <mh-order-details [order]="order" />
      }
      @if (!page.content.length) {
        <mh-empty
          title="Your first find is still out there."
          description="Orders will appear here after you check out."
          ><a class="button primary" routerLink="/products">Explore products</a></mh-empty
        >
      }
      <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
    }
  `,
})
export class OrdersPage {
  private readonly api = inject(OrdersApi);
  readonly resource = pagedResource((page, sort) => this.api.list(page, sort), ['createdAt,desc']);
}
