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
  template: ` <section class="checkout" aria-label="Checkout">
    <h2>{{ cart() ? 'Order summary' : 'Direct checkout' }}</h2>
    <p class="notice warning">
      Demo payments only. No real money is charged. Card and Google Pay simulate success; QR
      simulates a failed payment.
    </p>
    @if (order(); as result) {
      <div
        class="notice"
        [class.success]="result.status === 'PAID'"
        [class.error]="result.status !== 'PAID'"
        role="status"
      >
        <strong>{{
          result.status === 'PAID' ? 'Order paid' : 'Order status: ' + statusLabel(result.status)
        }}</strong>
        <p>Order {{ result.id }}</p>
        <p>{{ result.totalPriceCents | money }}</p>
        @if (result.status === 'PAYMENT_FAILED') {
          <p>Payment failed. Your cart has been kept.</p>
        }
        @if (result.status === 'COMPENSATION_FAILED') {
          <p>This order needs attention. Check your order history before placing another order.</p>
        }
      </div>
      <div class="actions">
        <a class="button primary" routerLink="/orders">View orders</a>
        @if (result.status === 'PAYMENT_FAILED') {
          <button class="button subtle" (click)="order.set(null)">Choose another method</button>
        }
      </div>
    } @else {
      <form [formGroup]="form" (ngSubmit)="submit()">
        <fieldset [disabled]="action.busy() || uncertain()">
          <label
            >Payment method<select [formControl]="payment">
              <option value="CARD">Card (simulation)</option>
              <option value="GOOGLE_PAY">Google Pay (simulation)</option>
              <option value="QR">QR (simulated failure)</option>
            </select></label
          >
          <div class="total">
            <span>Total</span><strong>{{ total() | money }}</strong>
          </div>
          <button
            class="button primary checkout-button"
            type="submit"
            [disabled]="disabled() || payment.invalid || (!cart() && !items().length)"
          >
            {{ action.busy() ? 'Placing order…' : 'Place simulated order' }}
          </button>
        </fieldset>
      </form>
      <mh-feedback [error]="action.error()" />
      @if (uncertain()) {
        <p class="notice warning">
          The outcome is uncertain. Check your orders before trying again to avoid placing a
          duplicate order.
        </p>
        <a class="button subtle" routerLink="/orders">Check order history</a>
      }
    }
  </section>`,
  styleUrl: './checkout.component.css',
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
