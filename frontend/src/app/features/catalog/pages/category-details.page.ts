import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../data-access/catalog-api.service';
import { detailResource } from '../../../shared/state/detail-resource';
import { FeedbackComponent, LoadingComponent } from '../../../shared/ui/feedback.component';
@Component({
  imports: [RouterLink, FeedbackComponent, LoadingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-details.page.html',
})
export class CategoryDetailsPage {
  private readonly api = inject(CatalogApi);
  readonly resource = detailResource((id) => this.api.category(id));
}
