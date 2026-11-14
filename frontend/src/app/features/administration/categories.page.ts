import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { AdminApi } from './data-access/admin-api.service';
import { Category } from '../catalog/data-access/catalog.models';
import { AdminNavComponent } from './admin-nav.component';
import { CategoryFormComponent } from './category-form.component';
import { pagedResource } from '../../shared/state/paged-resource';
import { ActionState } from '../../shared/state/request-state';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../shared/ui/feedback.component';
import { PaginationComponent } from '../../shared/ui/pagination.component';
import { ConfirmationComponent } from '../../shared/ui/confirmation.component';
@Component({
  imports: [
    AdminNavComponent,
    CategoryFormComponent,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
    ConfirmationComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './categories.page.html',
  styleUrl: './categories.page.scss',
})
export class AdminCategoriesPage {
  private readonly api = inject(AdminApi);
  readonly resource = pagedResource((page) => this.api.categories(page), ['name,asc']);
  readonly formBusy = signal(false);
  readonly action = new ActionState(inject(DestroyRef));
  readonly editing = signal<Category | null>(null);
  readonly toggling = signal<Category | null>(null);
  saved(): void {
    this.editing.set(null);
    this.action.message.set('Category saved.');
    this.resource.reload();
  }
  async toggle(category: Category): Promise<void> {
    this.toggling.set(null);
    await this.action.run(this.api.setCategoryEnabled(category.id, !category.active), () => {
      this.action.message.set(category.active ? 'Category deactivated.' : 'Category activated.');
      this.resource.reload();
    });
  }
}
