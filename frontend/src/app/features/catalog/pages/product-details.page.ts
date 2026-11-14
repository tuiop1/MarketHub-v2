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
    MoneyPipe,
    FieldErrorComponent,
    CheckoutComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-details.page.html',
  styleUrl: './product-details.page.scss',
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
