import { orderStatusLabel } from '../order-status';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  OrdersApi,
  Order,
  OrderStatus,
  PaymentMethod,
  PurchaseItem,
} from '../data-access/orders-api.service';
import { ActionState } from '../../../shared/state/request-state';
import { FeedbackComponent } from '../../../shared/ui/feedback.component';
import { MoneyPipe } from '../../../shared/ui/money.pipe';
@Component({
  selector: 'mh-checkout',
  imports: [ReactiveFormsModule, RouterLink, FeedbackComponent, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent {
  readonly items = input<readonly PurchaseItem[]>([]);
  readonly cart = input(false);
  readonly total = input(0);
  readonly disabled = input(false);
  readonly placed = output<Order>();
  readonly busyChange = output<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  readonly action = new ActionState(this.destroyRef);
  readonly order = signal<Order | null>(null);
  readonly uncertain = computed(() => {
    const error = this.action.error();
    return !!error && (error.status === 0 || error.status >= 500);
  });
  readonly payment = new FormControl<PaymentMethod>('CARD', {
    nonNullable: true,
    validators: Validators.required,
  });
  readonly form = new FormGroup({ payment: this.payment });
  private readonly api = inject(OrdersApi);
  statusLabel(status: OrderStatus): string {
    return orderStatusLabel(status);
  }
  async submit(): Promise<void> {
    if (
      this.action.busy() ||
      this.disabled() ||
      this.payment.invalid ||
      this.uncertain() ||
      (!this.cart() && !this.items().length)
    )
      return;
    this.busyChange.emit(true);
    await this.action.run(
      this.cart()
        ? this.api.purchaseCart(this.payment.value)
        : this.api.purchase(this.items(), this.payment.value),
      (order) => {
        this.order.set(order);
        this.placed.emit(order);
      }
    );
    if (!this.destroyRef.destroyed) this.busyChange.emit(false);
  }
}
