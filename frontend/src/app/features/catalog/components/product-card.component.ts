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
import { MoneyPipe } from '../../../shared/ui/money.pipe';
import { FeedbackComponent } from '../../../shared/ui/feedback.component';
import { FieldErrorComponent } from '../../../shared/ui/field-error.component';
import { integer } from '../../../shared/utilities/validators';
@Component({
  selector: 'mh-product-card',
  imports: [RouterLink, ReactiveFormsModule, MoneyPipe, FeedbackComponent, FieldErrorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
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
