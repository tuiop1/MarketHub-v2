import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../data-access/catalog-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { detailResource } from '../../../shared/state/detail-resource';
import { ActionState } from '../../../shared/state/request-state';
import { FeedbackComponent, LoadingComponent } from '../../../shared/ui/feedback.component';
import { ProductArtComponent } from '../../../shared/ui/product-art.component';
import { FieldErrorComponent } from '../../../shared/ui/field-error.component';
import { MoneyPipe } from '../../../shared/ui/money.pipe';
import { integer } from '../../../shared/utilities/validators';
import { CheckoutComponent } from '../../orders/components/checkout.component';
import { CartApi } from '../../cart/data-access/cart-api.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';
@Component({
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FeedbackComponent,
    LoadingComponent,
    ProductArtComponent,
    MoneyPipe,
    FieldErrorComponent,
    CheckoutComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <nav class="breadcrumb" aria-label="Breadcrumb">
      <a routerLink="/products">Marketplace</a><span>/</span><span>Product details</span>
    </nav>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading product…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as product) {
      <div class="product-details">
        <div>
          <mh-product-art [seed]="product.id" />
          <p class="u-text-muted u-text-small media-note">Product image unavailable</p>
        </div>
        <section>
          <span class="badge" [class.good]="product.stockQuantity > 0">{{
            product.stockQuantity > 0 ? product.stockQuantity + ' in stock' : 'Out of stock'
          }}</span>
          <h1>{{ product.name }}</h1>
          <p class="detail-price u-money">{{ product.priceCents | money }}</p>
          <p class="detail-copy">
            {{ product.description || 'No description has been provided for this product.' }}
          </p>
          <div class="actions">
            <a [routerLink]="['/merchants', product.merchantId]">View shop</a
            ><a [routerLink]="['/categories', product.categoryId]">View category</a>
          </div>
          @if (auth.hasRole('CUSTOMER')) {
            <form [formGroup]="form" (ngSubmit)="add()" class="purchase-controls">
              <fieldset [disabled]="checkoutBusy()">
                <label for="product-quantity"
                  >Quantity<input
                    id="product-quantity"
                    type="number"
                    [formControl]="quantity"
                    min="1"
                    [max]="product.stockQuantity"
                    step="1"
                    aria-describedby="quantity-error"
                /></label>
                <mh-field-error [control]="quantity" errorId="quantity-error" />
                <div class="actions">
                  <button
                    class="button primary"
                    type="submit"
                    [disabled]="action.busy() || checkoutBusy() || !buyable()"
                  >
                    {{ action.busy() ? 'Adding…' : 'Add to cart' }}</button><button
                    class="button subtle"
                    type="button"
                    [disabled]="checkoutBusy() || !buyable()"
                    (click)="openCheckout()"
                  >
                    Buy now
                  </button>
                </div>
              </fieldset>
            </form>
            <mh-feedback [error]="action.error()" [message]="action.message()" />
          } @else if (!auth.authenticated()) {
            <p class="notice">
              <a routerLink="/login" [queryParams]="{ returnTo: '/products/' + product.id }"
                >Sign in as a customer</a
              >
              to add this product to your cart or buy it.
            </p>
          }
        </section>
      </div>
      @if (checkoutQuantity(); as amount) {
        <div class="product-checkout">
          <mh-checkout
            [items]="[{ productId: product.id, quantity: amount }]"
            [total]="product.priceCents * amount"
            (busyChange)="checkoutBusy.set($event)"
          />
        </div>
      }
    }`,
  styleUrl: './product-details.page.css',
})
export class ProductDetailsPage {
  private readonly api = inject(CatalogApi);
  private readonly cart = inject(CartApi);
  readonly auth = inject(AuthService);
  readonly resource = detailResource((id) => this.api.product(id));
  readonly action = new ActionState(inject(DestroyRef));
  readonly quantity = new FormControl(1, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(1), integer],
  });
  readonly form = new FormGroup({ quantity: this.quantity });
  private readonly currentQuantity = toSignal(this.quantity.valueChanges.pipe(startWith(1)), {
    initialValue: 1,
  });
  readonly buyable = computed(() => {
    const product = this.resource.state().value;
    const quantity = this.currentQuantity();
    return (
      !!product &&
      product.active &&
      Number.isSafeInteger(quantity) &&
      quantity >= 1 &&
      quantity <= product.stockQuantity
    );
  });
  readonly checkoutQuantity = signal<number | null>(null);
  readonly checkoutBusy = signal(false);
  constructor() {
    effect(() => {
      const stock = this.resource.state().value?.stockQuantity;
      this.quantity.setValidators([
        Validators.required,
        Validators.min(1),
        integer,
        ...(stock === undefined ? [] : [Validators.max(stock)]),
      ]);
      this.quantity.updateValueAndValidity();
    });
  }
  async add(): Promise<void> {
    const product = this.resource.state().value;
    this.quantity.markAsTouched();
    if (!product || !this.buyable() || this.checkoutBusy()) return;
    await this.action.run(this.cart.add(product.id, this.quantity.value), () => {
      this.action.message.set('Added to your cart.');
    });
  }
  openCheckout(): void {
    this.quantity.markAsTouched();
    if (this.buyable()) this.checkoutQuantity.set(this.quantity.value);
  }
}
