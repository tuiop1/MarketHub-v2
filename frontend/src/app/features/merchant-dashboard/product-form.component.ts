import { Component, DestroyRef, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Product } from '../catalog/data-access/catalog.models';
import { MerchantProductsApi } from './data-access/merchant-products-api.service';
import { ActionState } from '../../shared/state/request-state';
import { FeedbackComponent } from '../../shared/ui/feedback.component';
import { FieldErrorComponent } from '../../shared/ui/field-error.component';
import { integer, nonBlank } from '../../shared/utilities/validators';
import { CategoryPickerComponent } from './category-picker.component';
@Component({
  selector: 'mh-product-form',
  imports: [ReactiveFormsModule, FeedbackComponent, FieldErrorComponent, CategoryPickerComponent],
  templateUrl: './product-form.component.html',
})
export class ProductFormComponent {
  readonly product = input<Product | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();
  readonly busyChange = output<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  readonly action = new ActionState(this.destroyRef);
  private readonly api = inject(MerchantProductsApi);
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly form = this.fb.group({
    name: ['', [Validators.required, nonBlank, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(5000)],
    priceCents: [
      1,
      [Validators.required, Validators.min(1), Validators.max(Number.MAX_SAFE_INTEGER), integer],
    ],
    stockQuantity: [
      0,
      [Validators.required, Validators.min(0), Validators.max(2147483647), integer],
    ],
    categoryId: ['', Validators.required],
    active: [true],
  });
  constructor() {
    effect(() => {
      const product = this.product();
      this.action.error.set(null);
      this.form.reset(
        product
          ? {
              name: product.name,
              description: product.description ?? '',
              priceCents: product.priceCents,
              stockQuantity: product.stockQuantity,
              categoryId: product.categoryId,
              active: product.active,
            }
          : undefined
      );
    });
  }
  async submit(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.action.busy()) return;
    const v = this.form.getRawValue();
    const create = {
      name: v.name.trim(),
      description: v.description.trim(),
      priceCents: v.priceCents,
      stockQuantity: v.stockQuantity,
      categoryId: v.categoryId,
    };
    const product = this.product();
    this.busyChange.emit(true);
    await this.action.run(
      product
        ? this.api.update(product.id, { ...create, active: v.active })
        : this.api.create(create),
      () => {
        this.form.reset();
        this.saved.emit();
      }
    );
    if (!this.destroyRef.destroyed) this.busyChange.emit(false);
  }
}
