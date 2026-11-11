import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../data-access/catalog.models';
import { CartApi } from '../../cart/data-access/cart-api.service';
import { PurchaseItem } from '../../orders/data-access/orders-api.service';
import { ActionState } from '../../../shared/state/request-state';
import { ProductArtComponent } from '../../../shared/ui/product-art.component';
import { MoneyPipe } from '../../../shared/ui/money.pipe';
import { FeedbackComponent } from '../../../shared/ui/feedback.component';
import { FieldErrorComponent } from '../../../shared/ui/field-error.component';
import { integer } from '../../../shared/utilities/validators';
@Component({
  selector: 'mh-product-card',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    ProductArtComponent,
    MoneyPipe,
    FeedbackComponent,
    FieldErrorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <article class="product-card" [class.selected]="selected()">
    <a
      [routerLink]="['/products', product().id]"
      class="art-link"
      [attr.aria-label]="'View ' + product().name"
      ><mh-product-art [seed]="product().id"
    /></a>
    <div class="card-copy">
      <div class="stock-line">
        <span>{{ product().stockQuantity > 0 ? 'Available' : 'Out of stock' }}</span><span
          >{{ product().stockQuantity }} in stock</span
        >
      </div>
      <h3>
        <a [routerLink]="['/products', product().id]">{{ product().name }}</a>
      </h3>
      <p class="description">
        {{ product().description || 'Discover this product in the marketplace.' }}
      </p>
      <div class="price-line">
        <strong class="u-money">{{ product().priceCents | money }}</strong><a
          [routerLink]="['/products', product().id]"
          >Details</a
        >
      </div>
      @if (customer()) {
        <form [formGroup]="form" (ngSubmit)="add()" class="cart-controls">
          <label [for]="'qty-' + product().id">Quantity</label>
          <div class="actions">
            <input
              type="number"
              [id]="'qty-' + product().id"
              [formControl]="quantity"
              min="1"
              [max]="product().stockQuantity"
              step="1"
              [attr.aria-describedby]="'qty-error-' + product().id"
              (input)="updateSelection()"
            />
            <button
              class="button subtle"
              type="submit"
              [disabled]="action.busy() || locked() || product().stockQuantity === 0"
            >
              {{ action.busy() ? 'Adding…' : 'Add to cart' }}
            </button>
          </div>
          <mh-field-error [control]="quantity" [errorId]="'qty-error-' + product().id" />
        </form>
        <label class="checkbox-label selection"
          ><input
            type="checkbox"
            [checked]="selected()"
            [disabled]="locked() || product().stockQuantity === 0"
            (change)="toggle($event)"
          />Select for direct checkout</label
        >
      }
      <mh-feedback [error]="action.error()" [message]="action.message()" />
    </div>
  </article>`,
  styleUrl: './product-card.component.css',
})
export class ProductCardComponent {
  readonly product = input.required<Product>();
  readonly customer = input(false);
  readonly selected = input(false);
  readonly locked = input(false);
  readonly selection = output<PurchaseItem | null>();
  readonly quantity = new FormControl(1, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(1), integer],
  });
  readonly form = new FormGroup({ quantity: this.quantity });
  readonly action = new ActionState(inject(DestroyRef));
  private readonly cart = inject(CartApi);
  private validQuantity(): boolean {
    this.quantity.markAsTouched();
    if (this.quantity.value > this.product().stockQuantity)
      this.quantity.setErrors({ max: { max: this.product().stockQuantity } });
    return this.quantity.valid && this.product().stockQuantity > 0;
  }
  toggle(event: Event): void {
    const checked = event.target instanceof HTMLInputElement && event.target.checked;
    if (checked && this.validQuantity())
      this.selection.emit({ productId: this.product().id, quantity: this.quantity.value });
    else {
      if (event.target instanceof HTMLInputElement) event.target.checked = false;
      this.selection.emit(null);
    }
  }
  updateSelection(): void {
    if (this.selected())
      this.selection.emit(
        this.validQuantity()
          ? { productId: this.product().id, quantity: this.quantity.value }
          : null
      );
  }
  async add(): Promise<void> {
    if (!this.customer() || this.locked() || !this.validQuantity()) return;
    await this.action.run(this.cart.add(this.product().id, this.quantity.value), () => {
      this.action.message.set('Added to your cart.');
    });
  }
}
