import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MerchantsApi } from '../data-access/merchants-api.service';
import { detailResource } from '../../../shared/state/detail-resource';
import { FeedbackComponent, LoadingComponent } from '../../../shared/ui/feedback.component';
@Component({
  styleUrl: './merchant-details.page.scss',
  imports: [RouterLink, DatePipe, FeedbackComponent, LoadingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './merchant-details.page.html',
})
export class MerchantDetailsPage {
  private readonly api = inject(MerchantsApi);
  readonly resource = detailResource((id) => this.api.get(id));
}
