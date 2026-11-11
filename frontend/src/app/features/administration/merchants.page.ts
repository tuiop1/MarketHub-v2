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
  template: `
    <div class="page-heading">
      <div>
        <h1>Shop applications.</h1>
        <p>Review new shops and manage approved sellers.</p>
      </div>
    </div>
    <mh-admin-nav />
    <nav class="tab-nav" aria-label="Shop status">
      <a
        [routerLink]="[]"
        [queryParams]="{ view: 'pending', page: 0 }"
        [class.active]="!verified()"
        [attr.aria-current]="!verified() ? 'page' : null"
        >Pending applications</a
      ><a
        [routerLink]="[]"
        [queryParams]="{ view: 'verified', page: 0 }"
        [class.active]="verified()"
        [attr.aria-current]="verified() ? 'page' : null"
        >Approved shops</a
      >
    </nav>
    <mh-feedback [error]="action.error()" [message]="action.message()" />
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading shops…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as page) {
      @if (page.content.length) {
        <div class="table-wrap">
          <table>
            <caption class="u-visually-hidden">
              {{
                verified() ? 'Approved shops' : 'Pending shop applications'
              }}
            </caption>
            <thead>
              <tr>
                <th scope="col">Shop</th>
                <th scope="col">Application</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (shop of page.content; track shop.id) {
                <tr>
                  <td>
                    <a class="cell-title" [routerLink]="['/merchants', shop.id]">{{
                      shop.shopName
                    }}</a>
                    <p>{{ shop.email }}</p>
                    <p class="u-text-muted">{{ shop.description }}</p>
                  </td>
                  <td>
                    <span class="badge" [class.good]="shop.status === 'VERIFIED'">{{
                      shop.status.toLowerCase()
                    }}</span>
                    <p class="u-text-small u-text-muted">
                      {{ shop.createdAt | date: 'mediumDate' }}
                    </p>
                  </td>
                  <td>
                    <div class="table-actions">
                      @if (shop.status === 'PENDING') {
                        <button
                          class="button primary"
                          [disabled]="action.busy()"
                          (click)="confirm(shop, 'verify')"
                        >
                          Approve</button
                        ><button
                          class="button danger"
                          [disabled]="action.busy()"
                          (click)="confirm(shop, 'reject')"
                        >
                          Reject
                        </button>
                      }
                      @if (shop.status === 'VERIFIED') {
                        <button
                          class="button danger"
                          [disabled]="action.busy()"
                          (click)="confirm(shop, 'disable')"
                        >
                          Suspend
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <mh-empty
          [title]="verified() ? 'No approved shops yet.' : 'No applications waiting.'"
          [description]="
            verified()
              ? 'Approved shops will appear here.'
              : 'You’re up to date. New applications will appear here.'
          "
        />
      }
      <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
    }
    @if (confirmation(); as target) {
      <mh-confirmation
        [title]="actionTitle(target.action) + ' shop?'"
        [message]="confirmationMessage()"
        [confirmLabel]="actionTitle(target.action) + ' shop'"
        (cancelled)="confirmation.set(null)"
        (confirmed)="perform()"
      />
    }
  `,
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
