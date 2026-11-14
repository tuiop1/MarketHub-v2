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
  templateUrl: './orders.page.html',
})
export class OrdersPage {
  private readonly api = inject(OrdersApi);
  readonly resource = pagedResource((page, sort) => this.api.list(page, sort), ['createdAt,desc']);
}
