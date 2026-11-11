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
  template: `
    <div class="page-heading">
      <div>
        <h1>Marketplace administration.</h1>
        <p>Keep the catalog organized and available.</p>
      </div>
    </div>
    <mh-admin-nav />
    <mh-feedback [error]="action.error()" [message]="action.message()" />
    <div class="admin-split">
      <section>
        <h2>Categories</h2>
        @if (resource.state().status === 'loading') {
          <mh-loading label="Loading categories…" />
        }
        <mh-feedback
          [error]="resource.state().error"
          [retryable]="true"
          (retry)="resource.reload()"
        />
        @if (resource.state().value; as page) {
          @if (page.content.length) {
            <div class="table-wrap">
              <table>
                <caption class="u-visually-hidden">
                  Marketplace categories
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Category</th>
                    <th scope="col">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (category of page.content; track category.id) {
                    <tr>
                      <td>
                        <span class="cell-title">{{ category.name }}</span><span
                          class="badge"
                          [class.good]="category.active"
                          >{{ category.active ? 'Active' : 'Inactive' }}</span
                        >
                        <p class="u-text-muted">{{ category.description }}</p>
                      </td>
                      <td>
                        <div class="table-actions">
                          <button
                            class="button subtle"
                            [disabled]="action.busy() || formBusy()"
                            (click)="editing.set(category)"
                          >
                            Edit</button
                          ><button
                            class="button subtle"
                            [disabled]="action.busy() || formBusy()"
                            (click)="toggling.set(category)"
                          >
                            {{ category.active ? 'Deactivate' : 'Activate' }}
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <mh-empty
              title="Create your first category."
              description="Merchants need an active category before they can add a product."
            />
          }
          <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
        }
      </section>
      <aside>
        <mh-category-form
          [category]="editing()"
          (busyChange)="formBusy.set($event)"
          (saved)="saved()"
          (cancelled)="editing.set(null)"
        />
      </aside>
    </div>
    @if (toggling(); as category) {
      <mh-confirmation
        [title]="category.active ? 'Deactivate category?' : 'Activate category?'"
        [message]="
          category.active
            ? 'Products in ' +
              category.name +
              ' will be hidden from public browsing until the category is active again.'
            : category.name + ' and its active products will be available in public browsing.'
        "
        [confirmLabel]="category.active ? 'Deactivate category' : 'Activate category'"
        (cancelled)="toggling.set(null)"
        (confirmed)="toggle(category)"
      />
    }
  `,
  styleUrl: './categories.page.css',
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
