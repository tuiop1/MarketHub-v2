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
  templateUrl: './categories.page.html',
})
export class CategoriesPage {
  private readonly api = inject(CatalogApi);
  readonly resource = pagedResource((page) => this.api.categories(page), ['name,asc']);
}
