import { orderStatusLabel } from '../order-status';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Order } from '../data-access/orders-api.service';
import { MoneyPipe } from '../../../shared/ui/money.pipe';
@Component({
  selector: 'mh-order-details',
  imports: [DatePipe, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './order-details.component.html',
  styleUrl: './order-details.component.scss',
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
