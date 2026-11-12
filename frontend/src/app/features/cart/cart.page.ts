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
  template: `
    <div class="page-heading">
      <div>
        <h1>Your cart.</h1>
        <p>A few good finds, ready when you are.</p>
      </div>
      <a class="button subtle" routerLink="/products">Continue browsing</a>
    </div>
    <mh-feedback [error]="action.error()" [message]="action.message()" />
    @if (state().status === 'loading') {
      <mh-loading label="Loading your cart…" />
    }
    <mh-feedback [error]="state().error" [retryable]="true" (retry)="reload.next()" />
    @if (state().status === 'error') {
      <p class="u-text-muted">
        An unavailable product can prevent your cart from loading. If refreshing does not help, you
        can clear the cart and start again.
      </p>
      <button class="button danger" [disabled]="action.busy()" (click)="confirmClear.set(true)">
        Clear unavailable cart
      </button>
    }
    <div class="split-layout">
      <section>
        @if (state().status === 'success' && !state().value?.cartItems?.length) {
          <mh-empty
            title="Room for something good."
            description="Your cart is empty. Explore the marketplace and add a product to get started."
            ><a class="button primary" routerLink="/products">Browse products</a></mh-empty
          >
        }
        @if (state().value; as cart) {
          @for (item of cart.cartItems; track item.id) {
            <mh-cart-item
              [item]="item"
              [busy]="action.busy() || checkoutBusy()"
              (increase)="increase(item)"
              (remove)="remove(item.id)"
            />
          }
          @if (cart.cartItems.length) {
            <button
              class="button link"
              [disabled]="action.busy() || checkoutBusy()"
              (click)="confirmClear.set(true)"
            >
              Clear cart
            </button>
            <p class="u-text-muted u-text-small">
              To reduce a quantity, remove the item and add the amount you need from its product
              page.
            </p>
          }
        }
      </section>
      <aside>
        @if (hasItems() || lastOrder()) {
          <mh-checkout
            [cart]="true"
            [total]="state().value?.totalPriceCents || lastOrder()?.totalPriceCents || 0"
            [disabled]="!canCheckout() || action.busy()"
            (busyChange)="checkoutBusy.set($event)"
            (placed)="onPlaced($event)"
          />
        }
      </aside>
    </div>
    @if (confirmClear()) {
      <mh-confirmation
        title="Clear your cart?"
        message="Every item will be removed from your cart. You can add products again from the marketplace."
        confirmLabel="Clear cart"
        (cancelled)="confirmClear.set(false)"
        (confirmed)="clear()"
      />
    }
  `,
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
