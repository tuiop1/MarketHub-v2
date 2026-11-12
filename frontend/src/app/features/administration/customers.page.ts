import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AdminApi } from './data-access/admin-api.service';
import { Customer } from '../account/data-access/account.models';
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
        <h1>Customer access.</h1>
        <p>Manage who can sign in to shop on MarketHub.</p>
      </div>
    </div>
    <mh-admin-nav />
    <mh-feedback [error]="action.error()" [message]="action.message()" />
    @if (resource.state().status === 'loading') {
      <mh-loading label="Loading customers…" />
    }
    <mh-feedback [error]="resource.state().error" [retryable]="true" (retry)="resource.reload()" />
    @if (resource.state().value; as page) {
      @if (page.content.length) {
        <div class="table-wrap">
          <table>
            <caption class="u-visually-hidden">
              Customer accounts
            </caption>
            <thead>
              <tr>
                <th scope="col">Customer</th>
                <th scope="col">Status</th>
                <th scope="col">Joined</th>
                <th scope="col">Action</th>
              </tr>
            </thead>
            <tbody>
              @for (customer of page.content; track customer.id) {
                <tr>
                  <td>
                    <span class="cell-title">{{ customer.firstName }} {{ customer.lastName }}</span>
                    <p>{{ customer.email }}</p>
                  </td>
                  <td>
                    <span
                      class="badge"
                      [class.good]="customer.enabled"
                      [class.bad]="!customer.enabled"
                      >{{ customer.enabled ? 'Enabled' : 'Disabled' }}</span
                    >
                  </td>
                  <td>{{ customer.createdAt | date: 'mediumDate' }}</td>
                  <td>
                    <button
                      class="button subtle"
                      [disabled]="action.busy()"
                      (click)="target.set(customer)"
                    >
                      {{ customer.enabled ? 'Disable' : 'Enable' }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <mh-empty
          title="No customer accounts yet."
          description="Registered customers will appear here."
        />
      }
      <mh-pagination [page]="page.number" [totalPages]="page.totalPages" />
    }
    @if (target(); as customer) {
      <mh-confirmation
        [title]="customer.enabled ? 'Disable customer?' : 'Enable customer?'"
        [message]="
          customer.enabled
            ? 'Disable sign-in for ' +
              customer.email +
              '? Existing access tokens may remain valid until they expire.'
            : 'Restore sign-in access for ' + customer.email + '?'
        "
        [confirmLabel]="customer.enabled ? 'Disable customer' : 'Enable customer'"
        (cancelled)="target.set(null)"
        (confirmed)="toggle(customer)"
      />
    }
  `,
})
export class AdminCustomersPage {
  private readonly api = inject(AdminApi);
  readonly resource = pagedResource((page) => this.api.customers(page), ['createdAt,desc']);
  readonly action = new ActionState(inject(DestroyRef));
  readonly target = signal<Customer | null>(null);
  async toggle(customer: Customer): Promise<void> {
    this.target.set(null);
    await this.action.run(this.api.setCustomerEnabled(customer.id, !customer.enabled), () => {
      this.action.message.set(customer.enabled ? 'Customer disabled.' : 'Customer enabled.');
      this.resource.reload();
    });
  }
}
