import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MerchantsApi } from '../data-access/merchants-api.service';
import { detailResource } from '../../../shared/state/detail-resource';
import { FeedbackComponent, LoadingComponent } from '../../../shared/ui/feedback.component';
@Component({
  styleUrl: './merchant-details.page.css',
  imports: [RouterLink, DatePipe, FeedbackComponent, LoadingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<nav class="breadcrumb" aria-label="Breadcrumb">
      <a routerLink="/merchants">Shops</a><span>/</span><span>Shop details</span>
    </nav>
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading shop…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as shop) {
      <section class="status-page">
        <span class="shop-avatar">{{ shop.shopName.slice(0, 1) }}</span>
        <h1 class="shop-name">{{ shop.shopName }}</h1>
        <span class="badge" [class.good]="shop.status === 'VERIFIED'">{{
          shop.status === 'VERIFIED' ? 'Approved shop' : shop.status.toLowerCase()
        }}</span>
        <p class="detail-copy shop-description">
          {{ shop.description || 'This shop has not added a description yet.' }}
        </p>
        <dl class="details-list">
          <dt>Contact</dt>
          <dd>{{ shop.email }}</dd>
          <dt>Joined</dt>
          <dd>{{ shop.createdAt | date: 'longDate' }}</dd>
        </dl>
        <a class="button primary" routerLink="/products">Explore the marketplace</a>
      </section>
    }`,
})
export class MerchantDetailsPage {
  private readonly api = inject(MerchantsApi);
  readonly resource = detailResource((id) => this.api.get(id));
}
