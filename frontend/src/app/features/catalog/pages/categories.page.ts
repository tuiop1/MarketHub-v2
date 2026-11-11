import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../data-access/catalog-api.service';
import { pagedResource } from '../../../shared/state/paged-resource';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../../shared/ui/feedback.component';
import { PaginationComponent } from '../../../shared/ui/pagination.component';
import { IconComponent } from '../../../shared/ui/icon.component';
@Component({
  imports: [
    RouterLink,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
    IconComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="page-heading">
      <div>
        <h1>Explore by category.</h1>
        <p>A place for every kind of find.</p>
      </div>
      <a class="button subtle" routerLink="/products">All products</a>
    </div>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading categories…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as page) {
      <div class="directory-grid">
        @for (category of page.content; track category.id) {
          <a class="directory-card" [routerLink]="['/categories', category.id]"
            ><mh-icon name="grid" />
            <h2>{{ category.name }}</h2>
            <p>{{ category.description || 'Explore this MarketHub category.' }}</p>
            <span class="link-label">View category</span></a
          >
        }
      </div>
      @if (!page.content.length) {
        <mh-empty
          title="Categories are on their way."
          description="Come back when shops have more to share."
        />
      }
      <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
    }`,
})
export class CategoriesPage {
  private readonly api = inject(CatalogApi);
  readonly resource = pagedResource((page) => this.api.categories(page), ['name,asc']);
}
