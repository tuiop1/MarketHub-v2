import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../data-access/catalog-api.service';
import { detailResource } from '../../../shared/state/detail-resource';
import { FeedbackComponent, LoadingComponent } from '../../../shared/ui/feedback.component';
@Component({
  imports: [RouterLink, FeedbackComponent, LoadingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<nav class="breadcrumb" aria-label="Breadcrumb">
      <a routerLink="/categories">Categories</a><span>/</span><span>Category details</span>
    </nav>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading category…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as category) {
      <section class="status-page">
        <h1>{{ category.name }}</h1>
        <p class="detail-copy">
          {{ category.description || 'This category has no description yet.' }}
        </p>
        <a class="button primary" routerLink="/products">Explore the marketplace</a>
      </section>
    }`,
})
export class CategoryDetailsPage {
  private readonly api = inject(CatalogApi);
  readonly resource = detailResource((id) => this.api.category(id));
}
