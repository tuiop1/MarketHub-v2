import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, startWith, switchMap } from 'rxjs';
import { CartApi, Cart, CartItem } from './data-access/cart-api.service';
import { CartItemComponent } from './cart-item.component';
import { CheckoutComponent } from '../orders/components/checkout.component';
import { Order } from '../orders/data-access/orders-api.service';
import { ActionState, loading, requestState } from '../../shared/state/request-state';
import {
  FeedbackComponent,
  EmptyComponent,
  LoadingComponent,
} from '../../shared/ui/feedback.component';
import { ConfirmationComponent } from '../../shared/ui/confirmation.component';
@Component({
  imports: [
    RouterLink,
    CartItemComponent,
    CheckoutComponent,
    FeedbackComponent,
    EmptyComponent,
    LoadingComponent,
    ConfirmationComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cart.page.html',
})
export class CartPage {
  private readonly api = inject(CartApi);
  readonly reload = new Subject<void>();
  readonly state = toSignal(
    this.reload.pipe(
      startWith(undefined),
      switchMap(() => requestState(this.api.get()))
    ),
    { initialValue: loading<Cart | null>() }
  );
  readonly action = new ActionState(inject(DestroyRef));
  readonly checkoutBusy = signal(false);
  readonly confirmClear = signal(false);
  readonly lastOrder = signal<Order | null>(null);
  readonly hasItems = computed(() => !!this.state().value?.cartItems.length);
  readonly canCheckout = computed(
    () =>
      this.state().status === 'success' &&
      this.hasItems() &&
      !!this.state().value?.cartItems.every(
        (item) =>
          item.productActive && item.priceCents != null && item.stockQuantity >= item.quantity
      )
  );
  async increase(item: CartItem): Promise<void> {
    if (this.checkoutBusy() || item.quantity >= item.stockQuantity) return;
    await this.action.run(this.api.add(item.productId, 1), () => {
      this.reload.next();
    });
  }
  async remove(id: string): Promise<void> {
    if (this.checkoutBusy()) return;
    await this.action.run(this.api.remove(id), () => {
      this.reload.next();
    });
  }
  async clear(): Promise<void> {
    this.confirmClear.set(false);
    if (this.checkoutBusy()) return;
    await this.action.run(this.api.clear(), () => {
      this.action.message.set('Cart cleared.');
      this.reload.next();
    });
  }
  onPlaced(order: Order): void {
    this.lastOrder.set(order);
    if (order.status === 'PAID') this.reload.next();
  }
}
