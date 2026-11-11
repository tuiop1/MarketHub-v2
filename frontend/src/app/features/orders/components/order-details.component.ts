import { orderStatusLabel } from '../order-status';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Order } from '../data-access/orders-api.service';
import { MoneyPipe } from '../../../shared/ui/money.pipe';
@Component({
  selector: 'mh-order-details',
  imports: [DatePipe, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <details class="order">
    <summary>
      <span class="order-meta"
        ><strong>Order {{ order().id.slice(0, 8) }}</strong
        ><span class="u-text-muted u-text-small">{{
          order().createdAt | date: 'medium'
        }}</span></span
      ><span class="badge" [class.good]="order().status === 'PAID'" [class.bad]="failed()">{{
        label()
      }}</span
      ><strong class="u-money">{{ order().totalPriceCents | money }}</strong
      ><span class="view-label">View details</span>
    </summary>
    <div class="order-content">
      <p class="u-text-small u-text-muted">Order ID: {{ order().id }}</p>
      @if (order().status === 'COMPENSATION_FAILED') {
        <p class="notice error">
          This order needs manual attention. Avoid placing another order until its outcome is
          confirmed.
        </p>
      }
      @if (order().status === 'PAYMENT_FAILED') {
        <p class="notice warning">Payment failed. This order was not paid.</p>
      }
      @if (order().status === 'PENDING_PAYMENT' || order().status === 'PENDING_RESERVATION') {
        <p class="notice">
          This order is still being processed. Refresh your orders to check its status.
        </p>
      }
      @for (item of order().items; track item.id) {
        <div class="order-line">
          <div>
            <strong>{{ item.productNameSnapshot }}</strong>
            <p class="u-text-small u-text-muted">{{ item.merchantNameSnapshot }}</p>
            <p class="u-text-small u-text-muted">
              {{ item.quantity }} × {{ item.priceSnapshotCents | money }}
            </p>
          </div>
          <span class="u-money">{{ item.totalPriceSnapshotCents | money }}</span>
        </div>
      }
    </div>
  </details>`,
  styleUrl: './order-details.component.css',
})
export class OrderDetailsComponent {
  readonly order = input.required<Order>();
  failed(): boolean {
    return ['PAYMENT_FAILED', 'CANCELLED', 'COMPENSATION_FAILED'].includes(this.order().status);
  }
  label(): string {
    return orderStatusLabel(this.order().status);
  }
}
