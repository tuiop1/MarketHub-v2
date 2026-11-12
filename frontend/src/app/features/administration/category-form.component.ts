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
  template: ` <form [formGroup]="form" (ngSubmit)="submit()" class="form-panel" novalidate>
    <h2>{{ category() ? 'Edit category' : 'Add a category' }}</h2>
    <fieldset [disabled]="action.busy()">
      <div class="form-grid">
        <label class="u-span-full" for="category-name"
          >Category name<input
            id="category-name"
            formControlName="name"
            maxlength="255"
            aria-describedby="category-name-error" /><mh-field-error
            [control]="form.controls.name"
            errorId="category-name-error"
            [serverError]="action.error()?.fields?.['name'] || ''"
        /></label>
        <label class="u-span-full" for="category-description"
          >Description<textarea
            id="category-description"
            formControlName="description"
            maxlength="1000"
            aria-describedby="category-description-error"
          ></textarea
          ><mh-field-error
            [control]="form.controls.description"
            errorId="category-description-error"
            [serverError]="action.error()?.fields?.['description'] || ''"
        /></label>
        <div class="actions u-span-full">
          <button class="button primary" type="submit">
            {{ action.busy() ? 'Saving…' : category() ? 'Save category' : 'Create category' }}
          </button>
          @if (category()) {
            <button class="button subtle" type="button" (click)="cancelled.emit()">
              Cancel edit
            </button>
          }
        </div>
      </div>
    </fieldset>
    <mh-feedback [error]="action.error()" />
  </form>`,
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
