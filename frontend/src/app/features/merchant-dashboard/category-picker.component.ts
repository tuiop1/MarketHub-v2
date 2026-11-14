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
  styleUrl: './category-picker.component.scss',
  selector: 'mh-category-picker',
  imports: [ReactiveFormsModule, FieldErrorComponent, FeedbackComponent],
  templateUrl: './category-picker.component.html',
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
