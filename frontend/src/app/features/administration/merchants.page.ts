import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AdminApi, MerchantAction } from './data-access/admin-api.service';
import { MerchantsApi } from '../merchants/data-access/merchants-api.service';
import { Merchant } from '../account/data-access/account.models';
import { AdminNavComponent } from './admin-nav.component';
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
    RouterLink,
    DatePipe,
    AdminNavComponent,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    PaginationComponent,
    ConfirmationComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './merchants.page.html',
})
export class AdminMerchantsPage {
  private readonly api = inject(AdminApi);
  private readonly shops = inject(MerchantsApi);
  private readonly query = toSignal(inject(ActivatedRoute).queryParamMap);
  readonly verified = computed(() => this.query()?.get('view') === 'verified');
  readonly resource = pagedResource(
    (page, _sort, view) =>
      view === 'verified'
        ? this.shops.list(page, 'createdAt,desc')
        : this.api.pendingMerchants(page),
    ['createdAt,asc']
  );
  readonly action = new ActionState(inject(DestroyRef));
  readonly confirmation = signal<{ shop: Merchant; action: MerchantAction } | null>(null);
  readonly confirmationMessage = computed(() => {
    const target = this.confirmation();
    if (!target) return '';
    return target.action === 'verify'
      ? `Approve ${target.shop.shopName} to sell products? The merchant will need a fresh sign-in or token to use their new access.`
      : target.action === 'reject'
        ? `Reject ${target.shop.shopName}'s application? There is no action to reverse this in MarketHub.`
        : `Suspend ${target.shop.shopName}? Their identity will be disabled. MarketHub has no re-enable action for shops.`;
  });
  actionTitle(action: MerchantAction): string {
    return { verify: 'Approve', reject: 'Reject', disable: 'Suspend' }[action];
  }
  confirm(shop: Merchant, action: MerchantAction): void {
    this.confirmation.set({ shop, action });
  }
  async perform(): Promise<void> {
    const target = this.confirmation();
    this.confirmation.set(null);
    if (!target) return;
    await this.action.run(this.api.merchantAction(target.shop.id, target.action), () => {
      this.action.message.set(
        `${target.shop.shopName}: ${this.actionTitle(target.action).toLowerCase()} action completed.`
      );
      this.resource.reload();
    });
  }
}
