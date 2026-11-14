import { Component, DestroyRef, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi } from './data-access/admin-api.service';
import { Category } from '../catalog/data-access/catalog.models';
import { ActionState } from '../../shared/state/request-state';
import { FieldErrorComponent } from '../../shared/ui/field-error.component';
import { FeedbackComponent } from '../../shared/ui/feedback.component';
import { nonBlank } from '../../shared/utilities/validators';
@Component({
  selector: 'mh-category-form',
  imports: [ReactiveFormsModule, FieldErrorComponent, FeedbackComponent],
  templateUrl: './category-form.component.html',
})
export class CategoryFormComponent {
  readonly category = input<Category | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();
  readonly busyChange = output<boolean>();
  private readonly api = inject(AdminApi);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly destroyRef = inject(DestroyRef);
  readonly action = new ActionState(this.destroyRef);
  readonly form = this.fb.group({
    name: ['', [Validators.required, nonBlank, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(1000)],
  });
  constructor() {
    effect(() => {
      const category = this.category();
      this.form.reset(
        category ? { name: category.name, description: category.description ?? '' } : undefined
      );
      this.action.error.set(null);
    });
  }
  async submit(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.action.busy()) return;
    const v = this.form.getRawValue();
    const body = { name: v.name.trim(), description: v.description.trim() };
    const category = this.category();
    this.busyChange.emit(true);
    await this.action.run(
      category ? this.api.updateCategory(category.id, body) : this.api.createCategory(body),
      () => {
        this.form.reset();
        this.saved.emit();
      }
    );
    if (!this.destroyRef.destroyed) this.busyChange.emit(false);
  }
}
