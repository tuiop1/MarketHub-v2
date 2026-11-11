import { Component, inject, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, switchMap } from 'rxjs';
import { CatalogApi } from '../catalog/data-access/catalog-api.service';
import { requestState, loading } from '../../shared/state/request-state';
import { Page } from '../../shared/models/page';
import { Category } from '../catalog/data-access/catalog.models';
import { FieldErrorComponent } from '../../shared/ui/field-error.component';
import { FeedbackComponent } from '../../shared/ui/feedback.component';
@Component({
  styleUrl: './category-picker.component.css',
  selector: 'mh-category-picker',
  imports: [ReactiveFormsModule, FieldErrorComponent, FeedbackComponent],
  template: `
    <label for="product-category"
      >Category<select
        id="product-category"
        [formControl]="control()"
        aria-describedby="category-error"
      >
        <option value="">Choose an active category</option>
        @if (control().value && !containsSelection()) {
          <option [value]="control().value">
            Selected category ({{ control().value.slice(0, 8) }})
          </option>
        }
        @for (category of state().value?.content || []; track category.id) {
          <option [value]="category.id">{{ category.name }}</option>
        }</select
      ><mh-field-error [control]="control()" errorId="category-error" [serverError]="serverError()"
    /></label>
    @if (state().status === 'loading') {
      <p class="u-text-muted u-text-small" role="status">Loading categories…</p>
    }
    <mh-feedback [error]="state().error" [retryable]="true" (retry)="page.next(page.value)" />
    @if (state().value; as result) {
      @if (result.totalPages > 1 || result.number > 0) {
        <div class="actions category-pagination">
          <button
            class="button link"
            type="button"
            [disabled]="result.first"
            (click)="page.next(result.number - 1)"
          >
            Previous categories</button
          ><span class="u-text-muted u-text-small"
            >{{ result.number + 1 }} / {{ result.totalPages }}</span
          ><button
            class="button link"
            type="button"
            [disabled]="result.last"
            (click)="page.next(result.number + 1)"
          >
            More categories
          </button>
        </div>
      }
      @if (!result.totalElements) {
        <p class="u-text-muted u-text-small">
          There are no active categories. A category is required to save a product.
        </p>
      }
    }
  `,
})
export class CategoryPickerComponent {
  readonly control = input.required<FormControl<string>>();
  readonly serverError = input('');
  private readonly api = inject(CatalogApi);
  readonly page = new BehaviorSubject(0);
  readonly state = toSignal(
    this.page.pipe(switchMap((page) => requestState(this.api.categories(page)))),
    { initialValue: loading<Page<Category>>() }
  );
  containsSelection(): boolean {
    return !!this.state().value?.content.some((c) => c.id === this.control().value);
  }
}
