import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MerchantsApi } from '../data-access/merchants-api.service';
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
  templateUrl: './merchants.page.html',
})
export class MerchantsPage {
  private readonly api = inject(MerchantsApi);
  private readonly router = inject(Router);
  readonly route = inject(ActivatedRoute);
  readonly resource = pagedResource(
    (page, sort) => this.api.list(page, sort),
    ['shopName,asc', 'createdAt,desc']
  );
  sort(event: Event): void {
    if (event.target instanceof HTMLSelectElement)
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { page: 0, sort: event.target.value },
        queryParamsHandling: 'merge',
      });
  }
}
