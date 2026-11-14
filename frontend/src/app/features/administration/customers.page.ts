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
  templateUrl: './customers.page.html',
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
